import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IService } from 'app/entities/service/service.model';
import { ServiceService } from 'app/entities/service/service/service.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { IPlanabonnement } from '../planabonnement.model';
import { PlanabonnementService } from '../service/planabonnement.service';
import { PlanabonnementFormService } from './planabonnement-form.service';

import { PlanabonnementUpdateComponent } from './planabonnement-update.component';

describe('Planabonnement Management Update Component', () => {
  let comp: PlanabonnementUpdateComponent;
  let fixture: ComponentFixture<PlanabonnementUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let planabonnementFormService: PlanabonnementFormService;
  let planabonnementService: PlanabonnementService;
  let serviceService: ServiceService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, PlanabonnementUpdateComponent],
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
      .overrideTemplate(PlanabonnementUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(PlanabonnementUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    planabonnementFormService = TestBed.inject(PlanabonnementFormService);
    planabonnementService = TestBed.inject(PlanabonnementService);
    serviceService = TestBed.inject(ServiceService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Service query and add missing value', () => {
      const planabonnement: IPlanabonnement = { id: 456 };
      const abpSevice: IService = { id: 24642 };
      planabonnement.abpSevice = abpSevice;

      const serviceCollection: IService[] = [{ id: 32227 }];
      jest.spyOn(serviceService, 'query').mockReturnValue(of(new HttpResponse({ body: serviceCollection })));
      const additionalServices = [abpSevice];
      const expectedCollection: IService[] = [...additionalServices, ...serviceCollection];
      jest.spyOn(serviceService, 'addServiceToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ planabonnement });
      comp.ngOnInit();

      expect(serviceService.query).toHaveBeenCalled();
      expect(serviceService.addServiceToCollectionIfMissing).toHaveBeenCalledWith(
        serviceCollection,
        ...additionalServices.map(expect.objectContaining),
      );
      expect(comp.servicesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Referentiel query and add missing value', () => {
      const planabonnement: IPlanabonnement = { id: 456 };
      const abptype: IReferentiel = { id: 16241 };
      planabonnement.abptype = abptype;

      const referentielCollection: IReferentiel[] = [{ id: 22738 }];
      jest.spyOn(referentielService, 'query').mockReturnValue(of(new HttpResponse({ body: referentielCollection })));
      const additionalReferentiels = [abptype];
      const expectedCollection: IReferentiel[] = [...additionalReferentiels, ...referentielCollection];
      jest.spyOn(referentielService, 'addReferentielToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ planabonnement });
      comp.ngOnInit();

      expect(referentielService.query).toHaveBeenCalled();
      expect(referentielService.addReferentielToCollectionIfMissing).toHaveBeenCalledWith(
        referentielCollection,
        ...additionalReferentiels.map(expect.objectContaining),
      );
      expect(comp.referentielsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const planabonnement: IPlanabonnement = { id: 456 };
      const abpSevice: IService = { id: 6906 };
      planabonnement.abpSevice = abpSevice;
      const abptype: IReferentiel = { id: 14799 };
      planabonnement.abptype = abptype;

      activatedRoute.data = of({ planabonnement });
      comp.ngOnInit();

      expect(comp.servicesSharedCollection).toContain(abpSevice);
      expect(comp.referentielsSharedCollection).toContain(abptype);
      expect(comp.planabonnement).toEqual(planabonnement);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPlanabonnement>>();
      const planabonnement = { id: 123 };
      jest.spyOn(planabonnementFormService, 'getPlanabonnement').mockReturnValue(planabonnement);
      jest.spyOn(planabonnementService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ planabonnement });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: planabonnement }));
      saveSubject.complete();

      // THEN
      expect(planabonnementFormService.getPlanabonnement).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(planabonnementService.update).toHaveBeenCalledWith(expect.objectContaining(planabonnement));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPlanabonnement>>();
      const planabonnement = { id: 123 };
      jest.spyOn(planabonnementFormService, 'getPlanabonnement').mockReturnValue({ id: null });
      jest.spyOn(planabonnementService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ planabonnement: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: planabonnement }));
      saveSubject.complete();

      // THEN
      expect(planabonnementFormService.getPlanabonnement).toHaveBeenCalled();
      expect(planabonnementService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPlanabonnement>>();
      const planabonnement = { id: 123 };
      jest.spyOn(planabonnementService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ planabonnement });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(planabonnementService.update).toHaveBeenCalled();
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

    describe('compareReferentiel', () => {
      it('Should forward to referentielService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(referentielService, 'compareReferentiel');
        comp.compareReferentiel(entity, entity2);
        expect(referentielService.compareReferentiel).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
