import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { ChoixService } from '../service/choix.service';
import { IChoix } from '../choix.model';
import { ChoixFormService } from './choix-form.service';

import { ChoixUpdateComponent } from './choix-update.component';

describe('Choix Management Update Component', () => {
  let comp: ChoixUpdateComponent;
  let fixture: ComponentFixture<ChoixUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let choixFormService: ChoixFormService;
  let choixService: ChoixService;
  let questionService: QuestionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ChoixUpdateComponent],
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
      .overrideTemplate(ChoixUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ChoixUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    choixFormService = TestBed.inject(ChoixFormService);
    choixService = TestBed.inject(ChoixService);
    questionService = TestBed.inject(QuestionService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Question query and add missing value', () => {
      const choix: IChoix = { id: 456 };
      const choquestion: IQuestion = { id: 12677 };
      choix.choquestion = choquestion;
      const choquestionSuivante: IQuestion = { id: 4347 };
      choix.choquestionSuivante = choquestionSuivante;

      const questionCollection: IQuestion[] = [{ id: 32483 }];
      jest.spyOn(questionService, 'query').mockReturnValue(of(new HttpResponse({ body: questionCollection })));
      const additionalQuestions = [choquestion, choquestionSuivante];
      const expectedCollection: IQuestion[] = [...additionalQuestions, ...questionCollection];
      jest.spyOn(questionService, 'addQuestionToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ choix });
      comp.ngOnInit();

      expect(questionService.query).toHaveBeenCalled();
      expect(questionService.addQuestionToCollectionIfMissing).toHaveBeenCalledWith(
        questionCollection,
        ...additionalQuestions.map(expect.objectContaining),
      );
      expect(comp.questionsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const choix: IChoix = { id: 456 };
      const choquestion: IQuestion = { id: 22127 };
      choix.choquestion = choquestion;
      const choquestionSuivante: IQuestion = { id: 17614 };
      choix.choquestionSuivante = choquestionSuivante;

      activatedRoute.data = of({ choix });
      comp.ngOnInit();

      expect(comp.questionsSharedCollection).toContain(choquestion);
      expect(comp.questionsSharedCollection).toContain(choquestionSuivante);
      expect(comp.choix).toEqual(choix);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IChoix>>();
      const choix = { id: 123 };
      jest.spyOn(choixFormService, 'getChoix').mockReturnValue(choix);
      jest.spyOn(choixService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ choix });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: choix }));
      saveSubject.complete();

      // THEN
      expect(choixFormService.getChoix).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(choixService.update).toHaveBeenCalledWith(expect.objectContaining(choix));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IChoix>>();
      const choix = { id: 123 };
      jest.spyOn(choixFormService, 'getChoix').mockReturnValue({ id: null });
      jest.spyOn(choixService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ choix: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: choix }));
      saveSubject.complete();

      // THEN
      expect(choixFormService.getChoix).toHaveBeenCalled();
      expect(choixService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IChoix>>();
      const choix = { id: 123 };
      jest.spyOn(choixService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ choix });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(choixService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareQuestion', () => {
      it('Should forward to questionService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(questionService, 'compareQuestion');
        comp.compareQuestion(entity, entity2);
        expect(questionService.compareQuestion).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
