import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IApplication } from 'app/entities/application/application.model';
import { ApplicationService } from 'app/entities/application/service/application.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IPlanabonnement } from 'app/entities/planabonnement/planabonnement.model';
import { PlanabonnementService } from 'app/entities/planabonnement/service/planabonnement.service';
import { IAbonnement } from '../abonnement.model';
import { AbonnementService } from '../service/abonnement.service';
import { AbonnementFormService } from './abonnement-form.service';

import { AbonnementUpdateComponent } from './abonnement-update.component';

describe('Abonnement Management Update Component', () => {
  let comp: AbonnementUpdateComponent;
  let fixture: ComponentFixture<AbonnementUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let abonnementFormService: AbonnementFormService;
  let abonnementService: AbonnementService;
  let applicationService: ApplicationService;
  let extendedUserService: ExtendedUserService;
  let planabonnementService: PlanabonnementService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, AbonnementUpdateComponent],
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
      .overrideTemplate(AbonnementUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(AbonnementUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    abonnementFormService = TestBed.inject(AbonnementFormService);
    abonnementService = TestBed.inject(AbonnementService);
    applicationService = TestBed.inject(ApplicationService);
    extendedUserService = TestBed.inject(ExtendedUserService);
    planabonnementService = TestBed.inject(PlanabonnementService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Application query and add missing value', () => {
      const abonnement: IAbonnement = { id: 456 };
      const application: IApplication = { id: 21707 };
      abonnement.application = application;

      const applicationCollection: IApplication[] = [{ id: 21631 }];
      jest.spyOn(applicationService, 'query').mockReturnValue(of(new HttpResponse({ body: applicationCollection })));
      const additionalApplications = [application];
      const expectedCollection: IApplication[] = [...additionalApplications, ...applicationCollection];
      jest.spyOn(applicationService, 'addApplicationToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      expect(applicationService.query).toHaveBeenCalled();
      expect(applicationService.addApplicationToCollectionIfMissing).toHaveBeenCalledWith(
        applicationCollection,
        ...additionalApplications.map(expect.objectContaining),
      );
      expect(comp.applicationsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call ExtendedUser query and add missing value', () => {
      const abonnement: IAbonnement = { id: 456 };
      const AboUser: IExtendedUser = { id: 5353 };
      abonnement.AboUser = AboUser;

      const extendedUserCollection: IExtendedUser[] = [{ id: 29583 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [AboUser];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Planabonnement query and add missing value', () => {
      const abonnement: IAbonnement = { id: 456 };
      const AboPlan: IPlanabonnement = { id: 11732 };
      abonnement.AboPlan = AboPlan;

      const planabonnementCollection: IPlanabonnement[] = [{ id: 21918 }];
      jest.spyOn(planabonnementService, 'query').mockReturnValue(of(new HttpResponse({ body: planabonnementCollection })));
      const additionalPlanabonnements = [AboPlan];
      const expectedCollection: IPlanabonnement[] = [...additionalPlanabonnements, ...planabonnementCollection];
      jest.spyOn(planabonnementService, 'addPlanabonnementToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      expect(planabonnementService.query).toHaveBeenCalled();
      expect(planabonnementService.addPlanabonnementToCollectionIfMissing).toHaveBeenCalledWith(
        planabonnementCollection,
        ...additionalPlanabonnements.map(expect.objectContaining),
      );
      expect(comp.planabonnementsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const abonnement: IAbonnement = { id: 456 };
      const application: IApplication = { id: 25149 };
      abonnement.application = application;
      const AboUser: IExtendedUser = { id: 26606 };
      abonnement.AboUser = AboUser;
      const AboPlan: IPlanabonnement = { id: 18797 };
      abonnement.AboPlan = AboPlan;

      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      expect(comp.applicationsSharedCollection).toContain(application);
      expect(comp.extendedUsersSharedCollection).toContain(AboUser);
      expect(comp.planabonnementsSharedCollection).toContain(AboPlan);
      expect(comp.abonnement).toEqual(abonnement);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAbonnement>>();
      const abonnement = { id: 123 };
      jest.spyOn(abonnementFormService, 'getAbonnement').mockReturnValue(abonnement);
      jest.spyOn(abonnementService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: abonnement }));
      saveSubject.complete();

      // THEN
      expect(abonnementFormService.getAbonnement).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(abonnementService.update).toHaveBeenCalledWith(expect.objectContaining(abonnement));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAbonnement>>();
      const abonnement = { id: 123 };
      jest.spyOn(abonnementFormService, 'getAbonnement').mockReturnValue({ id: null });
      jest.spyOn(abonnementService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ abonnement: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: abonnement }));
      saveSubject.complete();

      // THEN
      expect(abonnementFormService.getAbonnement).toHaveBeenCalled();
      expect(abonnementService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAbonnement>>();
      const abonnement = { id: 123 };
      jest.spyOn(abonnementService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ abonnement });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(abonnementService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareApplication', () => {
      it('Should forward to applicationService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(applicationService, 'compareApplication');
        comp.compareApplication(entity, entity2);
        expect(applicationService.compareApplication).toHaveBeenCalledWith(entity, entity2);
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

    describe('comparePlanabonnement', () => {
      it('Should forward to planabonnementService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(planabonnementService, 'comparePlanabonnement');
        comp.comparePlanabonnement(entity, entity2);
        expect(planabonnementService.comparePlanabonnement).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
