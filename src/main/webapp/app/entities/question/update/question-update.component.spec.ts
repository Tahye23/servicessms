import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { IQuestion } from '../question.model';
import { QuestionService } from '../service/question.service';
import { QuestionFormService } from './question-form.service';

import { QuestionUpdateComponent } from './question-update.component';

describe('Question Management Update Component', () => {
  let comp: QuestionUpdateComponent;
  let fixture: ComponentFixture<QuestionUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let questionFormService: QuestionFormService;
  let questionService: QuestionService;
  let companyService: CompanyService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, QuestionUpdateComponent],
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
      .overrideTemplate(QuestionUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(QuestionUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    questionFormService = TestBed.inject(QuestionFormService);
    questionService = TestBed.inject(QuestionService);
    companyService = TestBed.inject(CompanyService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Company query and add missing value', () => {
      const question: IQuestion = { id: 456 };
      const qusenquette: ICompany = { id: 19029 };
      question.qusenquette = qusenquette;

      const companyCollection: ICompany[] = [{ id: 9206 }];
      jest.spyOn(companyService, 'query').mockReturnValue(of(new HttpResponse({ body: companyCollection })));
      const additionalCompanies = [qusenquette];
      const expectedCollection: ICompany[] = [...additionalCompanies, ...companyCollection];
      jest.spyOn(companyService, 'addCompanyToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ question });
      comp.ngOnInit();

      expect(companyService.query).toHaveBeenCalled();
      expect(companyService.addCompanyToCollectionIfMissing).toHaveBeenCalledWith(
        companyCollection,
        ...additionalCompanies.map(expect.objectContaining),
      );
      expect(comp.companiesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Referentiel query and add missing value', () => {
      const question: IQuestion = { id: 456 };
      const qustypequestion: IReferentiel = { id: 31186 };
      question.qustypequestion = qustypequestion;

      const referentielCollection: IReferentiel[] = [{ id: 23643 }];
      jest.spyOn(referentielService, 'query').mockReturnValue(of(new HttpResponse({ body: referentielCollection })));
      const additionalReferentiels = [qustypequestion];
      const expectedCollection: IReferentiel[] = [...additionalReferentiels, ...referentielCollection];
      jest.spyOn(referentielService, 'addReferentielToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ question });
      comp.ngOnInit();

      expect(referentielService.query).toHaveBeenCalled();
      expect(referentielService.addReferentielToCollectionIfMissing).toHaveBeenCalledWith(
        referentielCollection,
        ...additionalReferentiels.map(expect.objectContaining),
      );
      expect(comp.referentielsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const question: IQuestion = { id: 456 };
      const qusenquette: ICompany = { id: 2811 };
      question.qusenquette = qusenquette;
      const qustypequestion: IReferentiel = { id: 14742 };
      question.qustypequestion = qustypequestion;

      activatedRoute.data = of({ question });
      comp.ngOnInit();

      expect(comp.companiesSharedCollection).toContain(qusenquette);
      expect(comp.referentielsSharedCollection).toContain(qustypequestion);
      expect(comp.question).toEqual(question);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IQuestion>>();
      const question = { id: 123 };
      jest.spyOn(questionFormService, 'getQuestion').mockReturnValue(question);
      jest.spyOn(questionService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ question });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: question }));
      saveSubject.complete();

      // THEN
      expect(questionFormService.getQuestion).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(questionService.update).toHaveBeenCalledWith(expect.objectContaining(question));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IQuestion>>();
      const question = { id: 123 };
      jest.spyOn(questionFormService, 'getQuestion').mockReturnValue({ id: null });
      jest.spyOn(questionService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ question: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: question }));
      saveSubject.complete();

      // THEN
      expect(questionFormService.getQuestion).toHaveBeenCalled();
      expect(questionService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IQuestion>>();
      const question = { id: 123 };
      jest.spyOn(questionService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ question });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(questionService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareCompany', () => {
      it('Should forward to companyService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(companyService, 'compareCompany');
        comp.compareCompany(entity, entity2);
        expect(companyService.compareCompany).toHaveBeenCalledWith(entity, entity2);
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
