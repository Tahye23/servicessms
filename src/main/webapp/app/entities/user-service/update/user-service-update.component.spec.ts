import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IService } from 'app/entities/service/service.model';
import { ServiceService } from 'app/entities/service/service/service.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IUserService } from '../user-service.model';
import { UserServiceService } from '../service/user-service.service';
import { UserServiceFormService } from './user-service-form.service';

import { UserServiceUpdateComponent } from './user-service-update.component';

describe('UserService Management Update Component', () => {
  let comp: UserServiceUpdateComponent;
  let fixture: ComponentFixture<UserServiceUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let userServiceFormService: UserServiceFormService;
  let userServiceService: UserServiceService;
  let serviceService: ServiceService;
  let extendedUserService: ExtendedUserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, UserServiceUpdateComponent],
      providers: [
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(UserServiceUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(UserServiceUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    userServiceFormService = TestBed.inject(UserServiceFormService);
    userServiceService = TestBed.inject(UserServiceService);
    serviceService = TestBed.inject(ServiceService);
    extendedUserService = TestBed.inject(ExtendedUserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Service query and add missing value', () => {
      const userService: IUserService = { id: 456 };
      const service: IService = { id: 27613 };
      userService.service = service;

      const serviceCollection: IService[] = [{ id: 4250 }];
      jest.spyOn(serviceService, 'query').mockReturnValue(of(new HttpResponse({ body: serviceCollection })));
      const additionalServices = [service];
      const expectedCollection: IService[] = [...additionalServices, ...serviceCollection];
      jest.spyOn(serviceService, 'addServiceToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ userService });
      comp.ngOnInit();

      expect(serviceService.query).toHaveBeenCalled();
      expect(serviceService.addServiceToCollectionIfMissing).toHaveBeenCalledWith(
        serviceCollection,
        ...additionalServices.map(expect.objectContaining),
      );
      expect(comp.servicesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call ExtendedUser query and add missing value', () => {
      const userService: IUserService = { id: 456 };
      const user: IExtendedUser = { id: 26872 };
      userService.user = user;

      const extendedUserCollection: IExtendedUser[] = [{ id: 24780 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [user];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ userService });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const userService: IUserService = { id: 456 };
      const service: IService = { id: 22396 };
      userService.service = service;
      const user: IExtendedUser = { id: 7394 };
      userService.user = user;

      activatedRoute.data = of({ userService });
      comp.ngOnInit();

      expect(comp.servicesSharedCollection).toContain(service);
      expect(comp.extendedUsersSharedCollection).toContain(user);
      expect(comp.userService).toEqual(userService);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserService>>();
      const userService = { id: 123 };
      jest.spyOn(userServiceFormService, 'getUserService').mockReturnValue(userService);
      jest.spyOn(userServiceService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userService });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: userService }));
      saveSubject.complete();

      // THEN
      expect(userServiceFormService.getUserService).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(userServiceService.update).toHaveBeenCalledWith(expect.objectContaining(userService));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserService>>();
      const userService = { id: 123 };
      jest.spyOn(userServiceFormService, 'getUserService').mockReturnValue({ id: null });
      jest.spyOn(userServiceService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userService: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: userService }));
      saveSubject.complete();

      // THEN
      expect(userServiceFormService.getUserService).toHaveBeenCalled();
      expect(userServiceService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserService>>();
      const userService = { id: 123 };
      jest.spyOn(userServiceService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userService });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(userServiceService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareService', () => {
      it('Should forward to serviceService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(serviceService, 'compareService');
        comp.compareService(entity, entity2);
        expect(serviceService.compareService).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareExtendedUser', () => {
      it('Should forward to extendedUserService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(extendedUserService, 'compareExtendedUser');
        comp.compareExtendedUser(entity, entity2);
        expect(extendedUserService.compareExtendedUser).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
