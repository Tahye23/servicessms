import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IApi } from 'app/entities/api/api.model';
import { ApiService } from 'app/entities/api/service/api.service';
import { IPlanabonnement } from 'app/entities/planabonnement/planabonnement.model';
import { PlanabonnementService } from 'app/entities/planabonnement/service/planabonnement.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IApplication } from '../application.model';
import { ApplicationService } from '../service/application.service';
import { ApplicationFormService } from './application-form.service';

import { ApplicationUpdateComponent } from './application-update.component';

describe('Application Management Update Component', () => {
  let comp: ApplicationUpdateComponent;
  let fixture: ComponentFixture<ApplicationUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let applicationFormService: ApplicationFormService;
  let applicationService: ApplicationService;
  let apiService: ApiService;
  let planabonnementService: PlanabonnementService;
  let extendedUserService: ExtendedUserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ApplicationUpdateComponent],
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
      .overrideTemplate(ApplicationUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    applicationFormService = TestBed.inject(ApplicationFormService);
    applicationService = TestBed.inject(ApplicationService);
    apiService = TestBed.inject(ApiService);
    planabonnementService = TestBed.inject(PlanabonnementService);
    extendedUserService = TestBed.inject(ExtendedUserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Api query and add missing value', () => {
      const application: IApplication = { id: 456 };
      const api: IApi = { id: 18668 };
      application.api = api;

      const apiCollection: IApi[] = [{ id: 25807 }];
      jest.spyOn(apiService, 'query').mockReturnValue(of(new HttpResponse({ body: apiCollection })));
      const additionalApis = [api];
      const expectedCollection: IApi[] = [...additionalApis, ...apiCollection];
      jest.spyOn(apiService, 'addApiToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ application });
      comp.ngOnInit();

      expect(apiService.query).toHaveBeenCalled();
      expect(apiService.addApiToCollectionIfMissing).toHaveBeenCalledWith(apiCollection, ...additionalApis.map(expect.objectContaining));
      expect(comp.apisSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Planabonnement query and add missing value', () => {
      const application: IApplication = { id: 456 };
      const planabonnement: IPlanabonnement = { id: 8768 };
      application.planabonnement = planabonnement;

      const planabonnementCollection: IPlanabonnement[] = [{ id: 10221 }];
      jest.spyOn(planabonnementService, 'query').mockReturnValue(of(new HttpResponse({ body: planabonnementCollection })));
      const additionalPlanabonnements = [planabonnement];
      const expectedCollection: IPlanabonnement[] = [...additionalPlanabonnements, ...planabonnementCollection];
      jest.spyOn(planabonnementService, 'addPlanabonnementToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ application });
      comp.ngOnInit();

      expect(planabonnementService.query).toHaveBeenCalled();
      expect(planabonnementService.addPlanabonnementToCollectionIfMissing).toHaveBeenCalledWith(
        planabonnementCollection,
        ...additionalPlanabonnements.map(expect.objectContaining),
      );
      expect(comp.planabonnementsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call ExtendedUser query and add missing value', () => {
      const application: IApplication = { id: 456 };
      const utilisateur: IExtendedUser = { id: 21364 };
      application.utilisateur = utilisateur;

      const extendedUserCollection: IExtendedUser[] = [{ id: 22669 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [utilisateur];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ application });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const application: IApplication = { id: 456 };
      const api: IApi = { id: 3598 };
      application.api = api;
      const planabonnement: IPlanabonnement = { id: 23656 };
      application.planabonnement = planabonnement;
      const utilisateur: IExtendedUser = { id: 27241 };
      application.utilisateur = utilisateur;

      activatedRoute.data = of({ application });
      comp.ngOnInit();

      expect(comp.apisSharedCollection).toContain(api);
      expect(comp.planabonnementsSharedCollection).toContain(planabonnement);
      expect(comp.extendedUsersSharedCollection).toContain(utilisateur);
      expect(comp.application).toEqual(application);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApplication>>();
      const application = { id: 123 };
      jest.spyOn(applicationFormService, 'getApplication').mockReturnValue(application);
      jest.spyOn(applicationService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ application });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: application }));
      saveSubject.complete();

      // THEN
      expect(applicationFormService.getApplication).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(applicationService.update).toHaveBeenCalledWith(expect.objectContaining(application));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApplication>>();
      const application = { id: 123 };
      jest.spyOn(applicationFormService, 'getApplication').mockReturnValue({ id: null });
      jest.spyOn(applicationService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ application: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: application }));
      saveSubject.complete();

      // THEN
      expect(applicationFormService.getApplication).toHaveBeenCalled();
      expect(applicationService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApplication>>();
      const application = { id: 123 };
      jest.spyOn(applicationService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ application });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(applicationService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareApi', () => {
      it('Should forward to apiService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(apiService, 'compareApi');
        comp.compareApi(entity, entity2);
        expect(apiService.compareApi).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('comparePlanabonnement', () => {
      it('Should forward to planabonnementService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(planabonnementService, 'comparePlanabonnement');
        comp.comparePlanabonnement(entity, entity2);
        expect(planabonnementService.comparePlanabonnement).toHaveBeenCalledWith(entity, entity2);
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
