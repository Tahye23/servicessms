import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { FileextraitService } from '../service/fileextrait.service';
import { IFileextrait } from '../fileextrait.model';
import { FileextraitFormService } from './fileextrait-form.service';

import { FileextraitUpdateComponent } from './fileextrait-update.component';

describe('Fileextrait Management Update Component', () => {
  let comp: FileextraitUpdateComponent;
  let fixture: ComponentFixture<FileextraitUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let fileextraitFormService: FileextraitFormService;
  let fileextraitService: FileextraitService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FileextraitUpdateComponent],
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
      .overrideTemplate(FileextraitUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(FileextraitUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    fileextraitFormService = TestBed.inject(FileextraitFormService);
    fileextraitService = TestBed.inject(FileextraitService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const fileextrait: IFileextrait = { id: 456 };

      activatedRoute.data = of({ fileextrait });
      comp.ngOnInit();

      expect(comp.fileextrait).toEqual(fileextrait);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IFileextrait>>();
      const fileextrait = { id: 123 };
      jest.spyOn(fileextraitFormService, 'getFileextrait').mockReturnValue(fileextrait);
      jest.spyOn(fileextraitService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ fileextrait });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: fileextrait }));
      saveSubject.complete();

      // THEN
      expect(fileextraitFormService.getFileextrait).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(fileextraitService.update).toHaveBeenCalledWith(expect.objectContaining(fileextrait));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IFileextrait>>();
      const fileextrait = { id: 123 };
      jest.spyOn(fileextraitFormService, 'getFileextrait').mockReturnValue({ id: null });
      jest.spyOn(fileextraitService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ fileextrait: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: fileextrait }));
      saveSubject.complete();

      // THEN
      expect(fileextraitFormService.getFileextrait).toHaveBeenCalled();
      expect(fileextraitService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IFileextrait>>();
      const fileextrait = { id: 123 };
      jest.spyOn(fileextraitService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ fileextrait });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(fileextraitService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
