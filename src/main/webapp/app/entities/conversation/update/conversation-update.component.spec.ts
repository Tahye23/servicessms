import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IQuestion } from 'app/entities/question/question.model';
import { QuestionService } from 'app/entities/question/service/question.service';
import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { IConversation } from '../conversation.model';
import { ConversationService } from '../service/conversation.service';
import { ConversationFormService } from './conversation-form.service';

import { ConversationUpdateComponent } from './conversation-update.component';

describe('Conversation Management Update Component', () => {
  let comp: ConversationUpdateComponent;
  let fixture: ComponentFixture<ConversationUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let conversationFormService: ConversationFormService;
  let conversationService: ConversationService;
  let contactService: ContactService;
  let questionService: QuestionService;
  let companyService: CompanyService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ConversationUpdateComponent],
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
      .overrideTemplate(ConversationUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ConversationUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    conversationFormService = TestBed.inject(ConversationFormService);
    conversationService = TestBed.inject(ConversationService);
    contactService = TestBed.inject(ContactService);
    questionService = TestBed.inject(QuestionService);
    companyService = TestBed.inject(CompanyService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Contact query and add missing value', () => {
      const conversation: IConversation = { id: 456 };
      const contact: IContact = { id: 834 };
      conversation.contact = contact;

      const contactCollection: IContact[] = [{ id: 30648 }];
      jest.spyOn(contactService, 'query').mockReturnValue(of(new HttpResponse({ body: contactCollection })));
      const additionalContacts = [contact];
      const expectedCollection: IContact[] = [...additionalContacts, ...contactCollection];
      jest.spyOn(contactService, 'addContactToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      expect(contactService.query).toHaveBeenCalled();
      expect(contactService.addContactToCollectionIfMissing).toHaveBeenCalledWith(
        contactCollection,
        ...additionalContacts.map(expect.objectContaining),
      );
      expect(comp.contactsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Question query and add missing value', () => {
      const conversation: IConversation = { id: 456 };
      const question: IQuestion = { id: 10508 };
      conversation.question = question;

      const questionCollection: IQuestion[] = [{ id: 28591 }];
      jest.spyOn(questionService, 'query').mockReturnValue(of(new HttpResponse({ body: questionCollection })));
      const additionalQuestions = [question];
      const expectedCollection: IQuestion[] = [...additionalQuestions, ...questionCollection];
      jest.spyOn(questionService, 'addQuestionToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      expect(questionService.query).toHaveBeenCalled();
      expect(questionService.addQuestionToCollectionIfMissing).toHaveBeenCalledWith(
        questionCollection,
        ...additionalQuestions.map(expect.objectContaining),
      );
      expect(comp.questionsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Company query and add missing value', () => {
      const conversation: IConversation = { id: 456 };
      const covenquette: ICompany = { id: 32709 };
      conversation.covenquette = covenquette;

      const companyCollection: ICompany[] = [{ id: 32069 }];
      jest.spyOn(companyService, 'query').mockReturnValue(of(new HttpResponse({ body: companyCollection })));
      const additionalCompanies = [covenquette];
      const expectedCollection: ICompany[] = [...additionalCompanies, ...companyCollection];
      jest.spyOn(companyService, 'addCompanyToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      expect(companyService.query).toHaveBeenCalled();
      expect(companyService.addCompanyToCollectionIfMissing).toHaveBeenCalledWith(
        companyCollection,
        ...additionalCompanies.map(expect.objectContaining),
      );
      expect(comp.companiesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Referentiel query and add missing value', () => {
      const conversation: IConversation = { id: 456 };
      const covstate: IReferentiel = { id: 20234 };
      conversation.covstate = covstate;

      const referentielCollection: IReferentiel[] = [{ id: 5991 }];
      jest.spyOn(referentielService, 'query').mockReturnValue(of(new HttpResponse({ body: referentielCollection })));
      const additionalReferentiels = [covstate];
      const expectedCollection: IReferentiel[] = [...additionalReferentiels, ...referentielCollection];
      jest.spyOn(referentielService, 'addReferentielToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      expect(referentielService.query).toHaveBeenCalled();
      expect(referentielService.addReferentielToCollectionIfMissing).toHaveBeenCalledWith(
        referentielCollection,
        ...additionalReferentiels.map(expect.objectContaining),
      );
      expect(comp.referentielsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const conversation: IConversation = { id: 456 };
      const contact: IContact = { id: 3231 };
      conversation.contact = contact;
      const question: IQuestion = { id: 23739 };
      conversation.question = question;
      const covenquette: ICompany = { id: 32036 };
      conversation.covenquette = covenquette;
      const covstate: IReferentiel = { id: 29990 };
      conversation.covstate = covstate;

      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      expect(comp.contactsSharedCollection).toContain(contact);
      expect(comp.questionsSharedCollection).toContain(question);
      expect(comp.companiesSharedCollection).toContain(covenquette);
      expect(comp.referentielsSharedCollection).toContain(covstate);
      expect(comp.conversation).toEqual(conversation);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IConversation>>();
      const conversation = { id: 123 };
      jest.spyOn(conversationFormService, 'getConversation').mockReturnValue(conversation);
      jest.spyOn(conversationService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: conversation }));
      saveSubject.complete();

      // THEN
      expect(conversationFormService.getConversation).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(conversationService.update).toHaveBeenCalledWith(expect.objectContaining(conversation));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IConversation>>();
      const conversation = { id: 123 };
      jest.spyOn(conversationFormService, 'getConversation').mockReturnValue({ id: null });
      jest.spyOn(conversationService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ conversation: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: conversation }));
      saveSubject.complete();

      // THEN
      expect(conversationFormService.getConversation).toHaveBeenCalled();
      expect(conversationService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IConversation>>();
      const conversation = { id: 123 };
      jest.spyOn(conversationService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ conversation });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(conversationService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareContact', () => {
      it('Should forward to contactService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(contactService, 'compareContact');
        comp.compareContact(entity, entity2);
        expect(contactService.compareContact).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareQuestion', () => {
      it('Should forward to questionService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(questionService, 'compareQuestion');
        comp.compareQuestion(entity, entity2);
        expect(questionService.compareQuestion).toHaveBeenCalledWith(entity, entity2);
      });
    });

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
