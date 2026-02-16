import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IReponse } from '../reponse.model';
import { ReponseService } from '../service/reponse.service';
import { ReponseFormService } from './reponse-form.service';

import { ReponseUpdateComponent } from './reponse-update.component';

describe('Reponse Management Update Component', () => {
  let comp: ReponseUpdateComponent;
  let fixture: ComponentFixture<ReponseUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let reponseFormService: ReponseFormService;
  let reponseService: ReponseService;
  let questionService: QuestionService;
  let contactService: ContactService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ReponseUpdateComponent],
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
      .overrideTemplate(ReponseUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ReponseUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    reponseFormService = TestBed.inject(ReponseFormService);
    reponseService = TestBed.inject(ReponseService);
    questionService = TestBed.inject(QuestionService);
    contactService = TestBed.inject(ContactService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Question query and add missing value', () => {
      const reponse: IReponse = { id: 456 };
      const repquestion: IQuestion = { id: 7365 };
      reponse.repquestion = repquestion;

      const questionCollection: IQuestion[] = [{ id: 10860 }];
      jest.spyOn(questionService, 'query').mockReturnValue(of(new HttpResponse({ body: questionCollection })));
      const additionalQuestions = [repquestion];
      const expectedCollection: IQuestion[] = [...additionalQuestions, ...questionCollection];
      jest.spyOn(questionService, 'addQuestionToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ reponse });
      comp.ngOnInit();

      expect(questionService.query).toHaveBeenCalled();
      expect(questionService.addQuestionToCollectionIfMissing).toHaveBeenCalledWith(
        questionCollection,
        ...additionalQuestions.map(expect.objectContaining),
      );
      expect(comp.questionsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Contact query and add missing value', () => {
      const reponse: IReponse = { id: 456 };
      const repcontact: IContact = { id: 4614 };
      reponse.repcontact = repcontact;

      const contactCollection: IContact[] = [{ id: 21162 }];
      jest.spyOn(contactService, 'query').mockReturnValue(of(new HttpResponse({ body: contactCollection })));
      const additionalContacts = [repcontact];
      const expectedCollection: IContact[] = [...additionalContacts, ...contactCollection];
      jest.spyOn(contactService, 'addContactToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ reponse });
      comp.ngOnInit();

      expect(contactService.query).toHaveBeenCalled();
      expect(contactService.addContactToCollectionIfMissing).toHaveBeenCalledWith(
        contactCollection,
        ...additionalContacts.map(expect.objectContaining),
      );
      expect(comp.contactsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const reponse: IReponse = { id: 456 };
      const repquestion: IQuestion = { id: 663 };
      reponse.repquestion = repquestion;
      const repcontact: IContact = { id: 12448 };
      reponse.repcontact = repcontact;

      activatedRoute.data = of({ reponse });
      comp.ngOnInit();

      expect(comp.questionsSharedCollection).toContain(repquestion);
      expect(comp.contactsSharedCollection).toContain(repcontact);
      expect(comp.reponse).toEqual(reponse);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReponse>>();
      const reponse = { id: 123 };
      jest.spyOn(reponseFormService, 'getReponse').mockReturnValue(reponse);
      jest.spyOn(reponseService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ reponse });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: reponse }));
      saveSubject.complete();

      // THEN
      expect(reponseFormService.getReponse).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(reponseService.update).toHaveBeenCalledWith(expect.objectContaining(reponse));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReponse>>();
      const reponse = { id: 123 };
      jest.spyOn(reponseFormService, 'getReponse').mockReturnValue({ id: null });
      jest.spyOn(reponseService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ reponse: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: reponse }));
      saveSubject.complete();

      // THEN
      expect(reponseFormService.getReponse).toHaveBeenCalled();
      expect(reponseService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IReponse>>();
      const reponse = { id: 123 };
      jest.spyOn(reponseService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ reponse });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(reponseService.update).toHaveBeenCalled();
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

    describe('compareContact', () => {
      it('Should forward to contactService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(contactService, 'compareContact');
        comp.compareContact(entity, entity2);
        expect(contactService.compareContact).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
