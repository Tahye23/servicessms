import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IGroupe } from 'app/entities/groupe/groupe.model';
import { GroupeService } from 'app/entities/groupe/service/groupe.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { ISendSms } from '../send-sms.model';
import { SendSmsService } from '../service/send-sms.service';
import { SendSmsFormService } from './send-sms-form.service';

import { SendSmsUpdateComponent } from './send-sms-update.component';

describe('SendSms Management Update Component', () => {
  let comp: SendSmsUpdateComponent;
  let fixture: ComponentFixture<SendSmsUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let sendSmsFormService: SendSmsFormService;
  let sendSmsService: SendSmsService;
  let extendedUserService: ExtendedUserService;
  let contactService: ContactService;
  let groupeService: GroupeService;
  let referentielService: ReferentielService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, SendSmsUpdateComponent],
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
      .overrideTemplate(SendSmsUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(SendSmsUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    sendSmsFormService = TestBed.inject(SendSmsFormService);
    sendSmsService = TestBed.inject(SendSmsService);
    extendedUserService = TestBed.inject(ExtendedUserService);
    contactService = TestBed.inject(ContactService);
    groupeService = TestBed.inject(GroupeService);
    referentielService = TestBed.inject(ReferentielService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call ExtendedUser query and add missing value', () => {
      const sendSms: ISendSms = { id: 456 };
      const user: IExtendedUser = { id: 31648 };
      sendSms.user = user;

      const extendedUserCollection: IExtendedUser[] = [{ id: 3136 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [user];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Contact query and add missing value', () => {
      const sendSms: ISendSms = { id: 456 };
      const destinateur: IContact = { id: 13489 };
      sendSms.destinateur = destinateur;

      const contactCollection: IContact[] = [{ id: 5288 }];
      jest.spyOn(contactService, 'query').mockReturnValue(of(new HttpResponse({ body: contactCollection })));
      const additionalContacts = [destinateur];
      const expectedCollection: IContact[] = [...additionalContacts, ...contactCollection];
      jest.spyOn(contactService, 'addContactToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      expect(contactService.query).toHaveBeenCalled();
      expect(contactService.addContactToCollectionIfMissing).toHaveBeenCalledWith(
        contactCollection,
        ...additionalContacts.map(expect.objectContaining),
      );
      expect(comp.contactsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Groupe query and add missing value', () => {
      const sendSms: ISendSms = { id: 456 };
      const destinataires: IGroupe = { id: 9906 };
      sendSms.destinataires = destinataires;

      const groupeCollection: IGroupe[] = [{ id: 11051 }];
      jest.spyOn(groupeService, 'query').mockReturnValue(of(new HttpResponse({ body: groupeCollection })));
      const additionalGroupes = [destinataires];
      const expectedCollection: IGroupe[] = [...additionalGroupes, ...groupeCollection];
      jest.spyOn(groupeService, 'addGroupeToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      expect(groupeService.query).toHaveBeenCalled();
      expect(groupeService.addGroupeToCollectionIfMissing).toHaveBeenCalledWith(
        groupeCollection,
        ...additionalGroupes.map(expect.objectContaining),
      );
      expect(comp.groupesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Referentiel query and add missing value', () => {
      const sendSms: ISendSms = { id: 456 };
      const Statut: IReferentiel = { id: 28010 };
      sendSms.Statut = Statut;

      const referentielCollection: IReferentiel[] = [{ id: 21019 }];
      jest.spyOn(referentielService, 'query').mockReturnValue(of(new HttpResponse({ body: referentielCollection })));
      const additionalReferentiels = [Statut];
      const expectedCollection: IReferentiel[] = [...additionalReferentiels, ...referentielCollection];
      jest.spyOn(referentielService, 'addReferentielToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      expect(referentielService.query).toHaveBeenCalled();
      expect(referentielService.addReferentielToCollectionIfMissing).toHaveBeenCalledWith(
        referentielCollection,
        ...additionalReferentiels.map(expect.objectContaining),
      );
      expect(comp.referentielsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const sendSms: ISendSms = { id: 456 };
      const user: IExtendedUser = { id: 200 };
      sendSms.user = user;
      const destinateur: IContact = { id: 7491 };
      sendSms.destinateur = destinateur;
      const destinataires: IGroupe = { id: 5479 };
      sendSms.destinataires = destinataires;
      const Statut: IReferentiel = { id: 24968 };
      sendSms.Statut = Statut;

      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      expect(comp.extendedUsersSharedCollection).toContain(user);
      expect(comp.contactsSharedCollection).toContain(destinateur);
      expect(comp.groupesSharedCollection).toContain(destinataires);
      expect(comp.referentielsSharedCollection).toContain(Statut);
      expect(comp.sendSms).toEqual(sendSms);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ISendSms>>();
      const sendSms = { id: 123 };
      jest.spyOn(sendSmsFormService, 'getSendSms').mockReturnValue(sendSms);
      jest.spyOn(sendSmsService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: sendSms }));
      saveSubject.complete();

      // THEN
      expect(sendSmsFormService.getSendSms).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(sendSmsService.update).toHaveBeenCalledWith(expect.objectContaining(sendSms));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ISendSms>>();
      const sendSms = { id: 123 };
      jest.spyOn(sendSmsFormService, 'getSendSms').mockReturnValue({ id: null });
      jest.spyOn(sendSmsService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ sendSms: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: sendSms }));
      saveSubject.complete();

      // THEN
      expect(sendSmsFormService.getSendSms).toHaveBeenCalled();
      expect(sendSmsService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ISendSms>>();
      const sendSms = { id: 123 };
      jest.spyOn(sendSmsService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ sendSms });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(sendSmsService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareExtendedUser', () => {
      it('Should forward to extendedUserService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(extendedUserService, 'compareExtendedUser');
        comp.compareExtendedUser(entity, entity2);
        expect(extendedUserService.compareExtendedUser).toHaveBeenCalledWith(entity, entity2);
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

    describe('compareGroupe', () => {
      it('Should forward to groupeService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(groupeService, 'compareGroupe');
        comp.compareGroupe(entity, entity2);
        expect(groupeService.compareGroupe).toHaveBeenCalledWith(entity, entity2);
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
