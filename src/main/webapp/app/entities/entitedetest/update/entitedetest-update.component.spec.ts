import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { EntitedetestService } from '../service/entitedetest.service';
import { IEntitedetest } from '../entitedetest.model';
import { EntitedetestFormService } from './entitedetest-form.service';

import { EntitedetestUpdateComponent } from './entitedetest-update.component';

describe('Entitedetest Management Update Component', () => {
  let comp: EntitedetestUpdateComponent;
  let fixture: ComponentFixture<EntitedetestUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let entitedetestFormService: EntitedetestFormService;
  let entitedetestService: EntitedetestService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, EntitedetestUpdateComponent],
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
      .overrideTemplate(EntitedetestUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(EntitedetestUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    entitedetestFormService = TestBed.inject(EntitedetestFormService);
    entitedetestService = TestBed.inject(EntitedetestService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const entitedetest: IEntitedetest = { id: 456 };

      activatedRoute.data = of({ entitedetest });
      comp.ngOnInit();

      expect(comp.entitedetest).toEqual(entitedetest);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IEntitedetest>>();
      const entitedetest = { id: 123 };
      jest.spyOn(entitedetestFormService, 'getEntitedetest').mockReturnValue(entitedetest);
      jest.spyOn(entitedetestService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ entitedetest });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: entitedetest }));
      saveSubject.complete();

      // THEN
      expect(entitedetestFormService.getEntitedetest).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(entitedetestService.update).toHaveBeenCalledWith(expect.objectContaining(entitedetest));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IEntitedetest>>();
      const entitedetest = { id: 123 };
      jest.spyOn(entitedetestFormService, 'getEntitedetest').mockReturnValue({ id: null });
      jest.spyOn(entitedetestService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ entitedetest: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: entitedetest }));
      saveSubject.complete();

      // THEN
      expect(entitedetestFormService.getEntitedetest).toHaveBeenCalled();
      expect(entitedetestService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IEntitedetest>>();
      const entitedetest = { id: 123 };
      jest.spyOn(entitedetestService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ entitedetest });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(entitedetestService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
