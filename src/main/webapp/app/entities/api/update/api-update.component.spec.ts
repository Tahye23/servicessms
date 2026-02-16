import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { ApiService } from '../service/api.service';
import { IApi } from '../api.model';
import { ApiFormService } from './api-form.service';

import { ApiUpdateComponent } from './api-update.component';

describe('Api Management Update Component', () => {
  let comp: ApiUpdateComponent;
  let fixture: ComponentFixture<ApiUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let apiFormService: ApiFormService;
  let apiService: ApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ApiUpdateComponent],
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
      .overrideTemplate(ApiUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ApiUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    apiFormService = TestBed.inject(ApiFormService);
    apiService = TestBed.inject(ApiService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const api: IApi = { id: 456 };

      activatedRoute.data = of({ api });
      comp.ngOnInit();

      expect(comp.api).toEqual(api);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApi>>();
      const api = { id: 123 };
      jest.spyOn(apiFormService, 'getApi').mockReturnValue(api);
      jest.spyOn(apiService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ api });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: api }));
      saveSubject.complete();

      // THEN
      expect(apiFormService.getApi).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(apiService.update).toHaveBeenCalledWith(expect.objectContaining(api));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApi>>();
      const api = { id: 123 };
      jest.spyOn(apiFormService, 'getApi').mockReturnValue({ id: null });
      jest.spyOn(apiService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ api: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: api }));
      saveSubject.complete();

      // THEN
      expect(apiFormService.getApi).toHaveBeenCalled();
      expect(apiService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IApi>>();
      const api = { id: 123 };
      jest.spyOn(apiService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ api });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(apiService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
