import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { ReferentielService } from '../service/referentiel.service';
import { IReferentiel } from '../referentiel.model';
import { ReferentielFormService } from './referentiel-form.service';

import { ReferentielUpdateComponent } from './referentiel-update.component';

describe('Referentiel Management Update Component', () => {
  let comp: ReferentielUpdateComponent;
  let fixture: ComponentFixture<ReferentielUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let referentielFormService: ReferentielFormService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ReferentielUpdateComponent],
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
      .overrideTemplate(ReferentielUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReferentielUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    referentielFormService = TestBed.inject(ReferentielFormService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const referentiel: IReferentiel = { id: 456 };

      activatedRoute.data = of({ referentiel });
      comp.ngOnInit();

      expect(comp.referentiel).toEqual(referentiel);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReferentiel>>();
      const referentiel = { id: 123 };
      jest.spyOn(referentielFormService, 'getReferentiel').mockReturnValue(referentiel);
      jest.spyOn(referentielService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ referentiel });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: referentiel }));
      saveSubject.complete();

      // THEN
      expect(referentielFormService.getReferentiel).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(referentielService.update).toHaveBeenCalledWith(expect.objectContaining(referentiel));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReferentiel>>();
      const referentiel = { id: 123 };
      jest.spyOn(referentielFormService, 'getReferentiel').mockReturnValue({ id: null });
      jest.spyOn(referentielService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ referentiel: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: referentiel }));
      saveSubject.complete();

      // THEN
      expect(referentielFormService.getReferentiel).toHaveBeenCalled();
      expect(referentielService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReferentiel>>();
      const referentiel = { id: 123 };
      jest.spyOn(referentielService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ referentiel });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(referentielService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
