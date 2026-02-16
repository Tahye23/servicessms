import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { ServiceService } from '../service/service.service';
import { IService } from '../service.model';
import { ServiceFormService } from './service-form.service';

import { ServiceUpdateComponent } from './service-update.component';

describe('Service Management Update Component', () => {
  let comp: ServiceUpdateComponent;
  let fixture: ComponentFixture<ServiceUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let serviceFormService: ServiceFormService;
  let serviceService: ServiceService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ServiceUpdateComponent],
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
      .overrideTemplate(ServiceUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ServiceUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    serviceFormService = TestBed.inject(ServiceFormService);
    serviceService = TestBed.inject(ServiceService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Referentiel query and add missing value', () => {
      const service: IService = { id: 456 };
      const accessType: IReferentiel = { id: 3975 };
      service.accessType = accessType;

      const referentielCollection: IReferentiel[] = [{ id: 15337 }];
      jest.spyOn(referentielService, 'query').mockReturnValue(of(new HttpResponse({ body: referentielCollection })));
      const additionalReferentiels = [accessType];
      const expectedCollection: IReferentiel[] = [...additionalReferentiels, ...referentielCollection];
      jest.spyOn(referentielService, 'addReferentielToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ service });
      comp.ngOnInit();

      expect(referentielService.query).toHaveBeenCalled();
      expect(referentielService.addReferentielToCollectionIfMissing).toHaveBeenCalledWith(
        referentielCollection,
        ...additionalReferentiels.map(expect.objectContaining),
      );
      expect(comp.referentielsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const service: IService = { id: 456 };
      const accessType: IReferentiel = { id: 15541 };
      service.accessType = accessType;

      activatedRoute.data = of({ service });
      comp.ngOnInit();

      expect(comp.referentielsSharedCollection).toContain(accessType);
      expect(comp.service).toEqual(service);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IService>>();
      const service = { id: 123 };
      jest.spyOn(serviceFormService, 'getService').mockReturnValue(service);
      jest.spyOn(serviceService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ service });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: service }));
      saveSubject.complete();

      // THEN
      expect(serviceFormService.getService).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(serviceService.update).toHaveBeenCalledWith(expect.objectContaining(service));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IService>>();
      const service = { id: 123 };
      jest.spyOn(serviceFormService, 'getService').mockReturnValue({ id: null });
      jest.spyOn(serviceService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ service: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: service }));
      saveSubject.complete();

      // THEN
      expect(serviceFormService.getService).toHaveBeenCalled();
      expect(serviceService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IService>>();
      const service = { id: 123 };
      jest.spyOn(serviceService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ service });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(serviceService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
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
