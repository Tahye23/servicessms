package com.example.myproject.service;

import com.example.myproject.config.Constants;
import com.example.myproject.domain.*;
import com.example.myproject.repository.*;
import com.example.myproject.security.AuthoritiesConstants;
import com.example.myproject.security.SecurityUtils;
import com.example.myproject.service.dto.AdminUserDTO;
import com.example.myproject.service.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private PlanabonnementRepository planAbonnementRepository; // Repository du plan

    @Autowired
    private AbonnementRepository abonnementRepository; // Repository abonnement

    private final AuthorityRepository authorityRepository;

    @Autowired
    private ExtendedUserRepository extendedUserRepository;

    private final CacheManager cacheManager;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorityRepository authorityRepository,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
    }

    public List<User> findUsersByLastName(String lastName) {
        return userRepository.findByLastName(lastName);
    }

    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository
            .findOneByActivationKey(key)
            .map(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                this.clearUserCaches(user);
                log.debug("Activated user: {}", user);
                return user;
            });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> findUsersFiltered(String status, String search, String roleFilter, Pageable pageable) {
        Boolean activatedStatus = null;
        if ("active".equalsIgnoreCase(status)) activatedStatus = true;
        else if ("inactive".equalsIgnoreCase(status)) activatedStatus = false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        boolean isPartner = roles.contains(AuthoritiesConstants.PARTNER);
        boolean isAdmin = roles.contains(AuthoritiesConstants.ADMIN);

        String filterExpediteur = null;
        String userRoleRestriction = null;

        if (isAdmin) {
            // ADMIN : voir tous les utilisateurs
            filterExpediteur = null;
            userRoleRestriction = null;
            // Mais si l'admin applique un filtre de rôle spécifique, on l'utilise
            // Le roleFilter du frontend sera directement utilisé dans la requête

        } else if (isPartner) {
            // PARTNER : voir seulement les utilisateurs avec le rôle USER et expediteur = son login
            filterExpediteur = auth.getName();
            userRoleRestriction = AuthoritiesConstants.USER; // Restriction obligatoire pour les partners
            // Si le partner essaie de filtrer par un autre rôle que USER, on ignore son filtre
            // car il ne peut voir que les USER de toute façon
        }

        // Déterminer le rôle final à filtrer
        String finalRoleFilter = null;

        if (isAdmin) {
            // L'admin peut filtrer par n'importe quel rôle
            finalRoleFilter = roleFilter;
        } else if (isPartner) {
            // Le partner ne peut voir que les USER, donc on force ce filtre
            finalRoleFilter = userRoleRestriction;
        }

        Page<User> users;

        // Choisir la requête selon si on a une recherche ou pas
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.findByFiltersWithSearch(filterExpediteur, activatedStatus, finalRoleFilter, search, pageable);
        } else {
            users = userRepository.findByFiltersWithoutSearch(filterExpediteur, activatedStatus, finalRoleFilter, pageable);
        }

        return users.map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableRoles() {
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

        return availableRoles;
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository
            .findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                this.clearUserCaches(user);
                return user;
            });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository
            .findOneByEmailIgnoreCase(mail)
            .filter(User::isActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                this.clearUserCaches(user);
                return user;
            });
    }

    public User registerUser(AdminUserDTO userDTO, String password) {
        // ... code existant pour vérifier login et email ...

        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            newUser.setEmail(userDTO.getEmail().toLowerCase());
        }
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        newUser.setExpediteur(userDTO.getExpediteur());
        newUser.setActivated(false);
        newUser.setActivationKey(RandomUtil.generateActivationKey());

        // Assigner le rôle PARTNER
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.PARTNER).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);

        // Assigner les permissions spécifiques
        newUser.setPermissions(getPartnerDefaultPermissions());

        userRepository.save(newUser);

        // Créer l'abonnement FREE depuis le plan en base
        createFreeSubscriptionFromPlan(newUser);

        this.clearUserCaches(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    private String getPartnerDefaultPermissions() {
        List<String> permissions = Arrays.asList(
            "canViewDashboard",
            "canSendSMS",
            "canSendWhatsApp",
            "canManageTemplates",
            "canManageContacts",
            "canManageGroups"
        );

        try {
            return new ObjectMapper().writeValueAsString(permissions);
        } catch (Exception e) {
            return "[]";
        }
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.isActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userRepository.flush();
        this.clearUserCaches(existingUser);
        return true;
    }

    private void createFreeSubscriptionFromPlan(User user) {
        try {
            // 1. Récupérer le plan FREE depuis la base (par nom ou type)
            PlanAbonnement freePlan = planAbonnementRepository
                .findByPlanType(PlanAbonnement.PlanType.FREE)
                .orElseThrow(() -> new RuntimeException("Plan FREE not found in database"));

            // 2. Récupérer ou créer l'ExtendedUser
            ExtendedUser extendedUser = extendedUserRepository
                .findByUser(user)
                .orElseGet(() -> {
                    ExtendedUser newExtendedUser = new ExtendedUser();
                    newExtendedUser.setUser(user);
                    newExtendedUser.setPhoneNumber(user.getPhone());
                    newExtendedUser.setPhoneNumber(user.getPhone());
                    return extendedUserRepository.save(newExtendedUser);
                });

            // 3. Créer l'abonnement basé sur le plan
            Abonnement freeSubscription = new Abonnement();
            freeSubscription.setUser(extendedUser);
            freeSubscription.setPlan(freePlan);
            freeSubscription.setStatus(Abonnement.SubscriptionStatus.ACTIVE);
            freeSubscription.setStartDate(LocalDate.now());
            freeSubscription.setEndDate(null); // FREE = illimité

            // 4. Copier les valeurs du plan FREE
            freeSubscription.setSmsUsed(0);
            freeSubscription.setWhatsappUsed(0);
            freeSubscription.setApiCallsToday(0);
            freeSubscription.setStorageUsedMb(0);

            // 5. Copier les permissions du plan
            freeSubscription.setCanViewDashboard(freePlan.getCanViewDashboard());
            freeSubscription.setCanManageAPI(freePlan.getCanManageAPI());
            freeSubscription.setCustomCanManageUsers(freePlan.getCanManageUsers());
            freeSubscription.setCustomCanManageTemplates(freePlan.getCanManageTemplates());
            freeSubscription.setCustomCanViewConversations(freePlan.getCanViewConversations());
            freeSubscription.setCustomCanViewAnalytics(freePlan.getCanViewAnalytics());
            freeSubscription.setCustomPrioritySupport(freePlan.getPrioritySupport());

            // 6. Copier les limites du plan
            freeSubscription.setCustomSmsLimit(freePlan.getSmsLimit());
            freeSubscription.setCustomWhatsappLimit(freePlan.getWhatsappLimit());
            freeSubscription.setCustomUsersLimit(freePlan.getUsersLimit());
            freeSubscription.setCustomTemplatesLimit(freePlan.getTemplatesLimit());
            freeSubscription.setCustomApiCallsLimit(freePlan.getMaxApiCallsPerDay());
            freeSubscription.setCustomStorageLimitMb(freePlan.getStorageLimitMb());

            // 7. Plan non custom
            freeSubscription.setCustomPlan(false);
            freeSubscription.setTrial(false);
            freeSubscription.setAutoRenew(true);
            freeSubscription.setSidebarVisible(true);

            // 8. Sauvegarder
            abonnementRepository.save(freeSubscription);

            log.debug("Created FREE subscription from plan for user: {}", user.getLogin());
        } catch (Exception e) {
            log.error("Error creating FREE subscription for user: {}", user.getLogin(), e);
            throw new RuntimeException("Failed to create FREE subscription", e);
        }
    }

    public User createUser(AdminUserDTO userDTO, Boolean isPartner, Boolean isAdmin) {
        User user = new User();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setPhone(userDTO.getPhone());
        user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        if (isPartner && !isAdmin) {
            Long currentPartnerId = SecurityUtils.getCurrentUserId().orElseThrow(() -> new RuntimeException("No authenticated user found"));
            user.setPartnerUserId(currentPartnerId);
        }
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setPartnerUserId(userDTO.getPartnerUserId());
        user.setPermissions(userDTO.getPermissions());
        user.setExpediteur(userDTO.getExpediteur());
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        if (userDTO.getAuthorities() != null) {
            Set<Authority> authorities = userDTO
                .getAuthorities()
                .stream()
                .map(authorityRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
            user.setAuthorities(authorities);
        }
        userRepository.save(user);
        this.clearUserCaches(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    public Optional<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return Optional.of(userRepository.findById(userDTO.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(user -> {
                this.clearUserCaches(user);
                user.setLogin(userDTO.getLogin().toLowerCase());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());
                if (userDTO.getEmail() != null) {
                    user.setEmail(userDTO.getEmail().toLowerCase());
                }
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                user.setPhone(userDTO.getPhone());
                user.setExpediteur(userDTO.getExpediteur());
                user.setPermissions(userDTO.getPermissions());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                userDTO
                    .getAuthorities()
                    .stream()
                    .map(authorityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(managedAuthorities::add);
                userRepository.save(user);
                this.clearUserCaches(user);
                log.debug("Changed Information for User: {}", user);
                return user;
            })
            .map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getUsersByExpediteurAndAuthority(String expediteur, String authority, Pageable pageable) {
        return userRepository.findAllByExpediteurAndAuthorities_Name(expediteur, authority, pageable).map(AdminUserDTO::new);
    }

    public void deleteUser(String login) {
        userRepository
            .findOneByLogin(login)
            .ifPresent(user -> {
                userRepository.delete(user);
                this.clearUserCaches(user);
                log.debug("Deleted User: {}", user);
            });
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     */
    public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                if (email != null) {
                    user.setEmail(email.toLowerCase());
                }
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);
                userRepository.save(user);
                this.clearUserCaches(user);
                log.debug("Changed Information for User: {}", user);
            });
    }

    @Transactional
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .ifPresent(user -> {
                String currentEncryptedPassword = user.getPassword();
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw new InvalidPasswordException();
                }
                String encryptedPassword = passwordEncoder.encode(newPassword);
                user.setPassword(encryptedPassword);
                this.clearUserCaches(user);
                log.debug("Changed password for User: {}", user);
            });
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllPublicUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable).map(UserDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS))
            .forEach(user -> {
                log.debug("Deleting not activated user {}", user.getLogin());
                userRepository.delete(user);
                this.clearUserCaches(user);
            });
    }

    /**
     * Gets a list of all the authorities.
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).toList();
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evict(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evict(user.getEmail());
        }
    }
}
