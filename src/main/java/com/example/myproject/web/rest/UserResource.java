package com.example.myproject.web.rest;

import com.example.myproject.config.Constants;
import com.example.myproject.domain.Authority;
import com.example.myproject.domain.User;
import com.example.myproject.repository.UserRepository;
import com.example.myproject.security.AuthoritiesConstants;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.MailService;
import com.example.myproject.service.UserService;
import com.example.myproject.service.dto.AdminUserDTO;
import com.example.myproject.web.rest.errors.BadRequestAlertException;
import com.example.myproject.web.rest.errors.EmailAlreadyUsedException;
import com.example.myproject.web.rest.errors.LoginAlreadyUsedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Collections;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing users.
 * <p>
 * This class accesses the {@link com.example.myproject.domain.User} entity, and needs to fetch its collection of authorities.
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between User and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will
 * quite often do relationships with the user, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our users'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>
 * Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api/admin")
public class UserResource {

    private static final List<String> ALLOWED_ORDERED_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            "id",
            "login",
            "firstName",
            "lastName",
            "email",
            "activated",
            "langKey",
            "createdBy",
            "createdDate",
            "lastModifiedBy",
            "lastModifiedDate"
        )
    );

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserService userService;

    private final UserRepository userRepository;

    private final MailService mailService;

    public ResponseEntity<List<User>> getUsersByLastName(@RequestParam String lastName) {
        List<User> users = userService.findUsersByLastName(lastName);
        return ResponseEntity.ok(users);
    }

    public UserResource(UserService userService, UserRepository userRepository, MailService mailService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    /**
     * {@code POST  /admin/users}  : Creates a new user.
     * <p>
     * Creates a new user if the login and email are not already used, and sends an
     * mail with an activation link.
     * The user needs to be activated on creation.
     *
     * @param userDTO the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new user, or with status {@code 400 (Bad Request)} if the login or email is already in use.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     * @throws BadRequestAlertException {@code 400 (Bad Request)} if the login or email is already in use.
     */
    @PostMapping("/users")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<User> createUser(@Valid @RequestBody AdminUserDTO userDTO) throws URISyntaxException {
        log.info("REST request to save User : {}", userDTO);
        String login = getCurrentLoginOrThrow();
        Long idUser = getCurrentIdOrThrow();
        if (userDTO.getId() != null) {
            throw new BadRequestAlertException("A new user cannot already have an ID", "userManagement", "idexists");
        }
        if (userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).isPresent()) {
            throw new LoginAlreadyUsedException();
        }
        if (userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException();
        }

        // Récupérer l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        boolean isPartner = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.PARTNER));

        // Si PARTNER et non ADMIN, vérifier le rôle de l'utilisateur à créer
        if (isPartner && !isAdmin) {
            // On autorise uniquement le rôle USER
            userDTO.setExpediteur(login);
            userDTO.setPartnerUserId(idUser);
            if (!userDTO.getAuthorities().equals(Set.of(AuthoritiesConstants.USER))) {
                throw new BadRequestAlertException(
                    "Partner can only create users with USER authority",
                    "userManagement",
                    "authoritynotallowed"
                );
            }
        }

        // Sinon ADMIN peut créer n'importe quel type d'utilisateur
        User newUser = userService.createUser(userDTO, isPartner, isAdmin);
        mailService.sendCreationEmail(newUser);
        return ResponseEntity.created(new URI("/api/admin/users/" + newUser.getLogin()))
            .headers(HeaderUtil.createAlert(applicationName, "userManagement.created", newUser.getLogin()))
            .body(newUser);
    }

    private String getCurrentLoginOrThrow() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Non authentifié"));
    }

    private Long getCurrentIdOrThrow() {
        return SecurityUtils.getCurrentUserId().orElseThrow(() -> new IllegalStateException("Non authentifié"));
    }

    /**
     * {@code PUT /admin/users} : Updates an existing User.
     *
     * @param userDTO the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated user.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already in use.
     * @throws LoginAlreadyUsedException {@code 400 (Bad Request)} if the login is already in use.
     */
    @PutMapping({ "/users", "/users/{login}" })
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<AdminUserDTO> updateUser(
        @PathVariable(name = "login", required = false) @Pattern(regexp = Constants.LOGIN_REGEX) String login,
        @Valid @RequestBody AdminUserDTO userDTO
    ) {
        log.debug("REST request to update User : {}", userDTO);
        String loginUser = getCurrentLoginOrThrow();
        Optional<User> existingUserByEmail = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUserByEmail.isPresent() && (!existingUserByEmail.get().getId().equals(userDTO.getId()))) {
            throw new EmailAlreadyUsedException();
        }

        Optional<User> existingUserByLogin = userRepository.findOneByLogin(userDTO.getLogin().toLowerCase());
        if (existingUserByLogin.isPresent() && (!existingUserByLogin.get().getId().equals(userDTO.getId()))) {
            throw new LoginAlreadyUsedException();
        }

        // Vérification des autorités de l'utilisateur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.ADMIN));
        boolean isPartner = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(AuthoritiesConstants.PARTNER));

        if (isPartner && !isAdmin) {
            userDTO.setExpediteur(loginUser);
        }

        // Effectuer la mise à jour
        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO);

        return ResponseUtil.wrapOrNotFound(
            updatedUser,
            HeaderUtil.createAlert(applicationName, "userManagement.updated", userDTO.getLogin())
        );
    }

    /**
     * {@code GET /admin/users} : get all users with all the details - calling this are only allowed for the administrators.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all users.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role
    ) {
        log.debug("REST request to get all Users with filters - status: {}, search: {}, role: {}", status, search, role);

        if (!onlyContainsAllowedProperties(pageable)) {
            return ResponseEntity.badRequest().build();
        }

        Page<AdminUserDTO> page = userService.findUsersFiltered(status, search, role, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    // Dans le contrôleur UserResource

    @GetMapping("/users/roles")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<List<String>> getAvailableRoles() {
        log.debug("REST request to get available roles");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        List<String> availableRoles = new ArrayList<>();

        if (roles.contains(AuthoritiesConstants.ADMIN)) {
            // L'admin peut filtrer par tous les rôles
            availableRoles.add(AuthoritiesConstants.ADMIN);
            availableRoles.add(AuthoritiesConstants.PARTNER);
            availableRoles.add(AuthoritiesConstants.USER);
        } else if (roles.contains(AuthoritiesConstants.PARTNER)) {
            // Le partner ne peut voir que les USER
            availableRoles.add(AuthoritiesConstants.USER);
        }

        return ResponseEntity.ok(availableRoles);
    }

    // Ou dans le service UserService

    private boolean onlyContainsAllowedProperties(Pageable pageable) {
        return pageable.getSort().stream().map(Sort.Order::getProperty).allMatch(ALLOWED_ORDERED_PROPERTIES::contains);
    }

    /**
     * {@code GET /admin/users/:login} : get the "login" user.
     *
     * @param login the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/users/{login}")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable("login") @Pattern(regexp = Constants.LOGIN_REGEX) String login) {
        log.debug("REST request to get User : {}", login);
        return ResponseUtil.wrapOrNotFound(userService.getUserWithAuthoritiesByLogin(login).map(AdminUserDTO::new));
    }

    /**
     * {@code DELETE /admin/users/:login} : delete the "login" User.
     *
     * @param login the login of the user to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/users/{login}")
    @PreAuthorize("hasAnyAuthority('" + AuthoritiesConstants.ADMIN + "', '" + AuthoritiesConstants.PARTNER + "')")
    public ResponseEntity<Void> deleteUser(@PathVariable("login") @Pattern(regexp = Constants.LOGIN_REGEX) String login) {
        log.debug("REST request to delete User: {}", login);
        userService.deleteUser(login);
        return ResponseEntity.noContent().headers(HeaderUtil.createAlert(applicationName, "userManagement.deleted", login)).build();
    }
}
