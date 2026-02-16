import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { ICompany } from 'app/entities/company/company.model';
import { CompanyService } from 'app/entities/company/service/company.service';
import { IParticipant } from '../participant.model';
import { ParticipantService } from '../service/participant.service';
import { ParticipantFormService } from './participant-form.service';

import { ParticipantUpdateComponent } from './participant-update.component';

describe('Participant Management Update Component', () => {
  let comp: ParticipantUpdateComponent;
  let fixture: ComponentFixture<ParticipantUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let participantFormService: ParticipantFormService;
  let participantService: ParticipantService;
  let contactService: ContactService;
  let companyService: CompanyService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ParticipantUpdateComponent],
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
      .overrideTemplate(ParticipantUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ParticipantUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    participantFormService = TestBed.inject(ParticipantFormService);
    participantService = TestBed.inject(ParticipantService);
    contactService = TestBed.inject(ContactService);
    companyService = TestBed.inject(CompanyService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Contact query and add missing value', () => {
      const participant: IParticipant = { id: 456 };
      const patcontact: IContact = { id: 4240 };
      participant.patcontact = patcontact;

      const contactCollection: IContact[] = [{ id: 22675 }];
      jest.spyOn(contactService, 'query').mockReturnValue(of(new HttpResponse({ body: contactCollection })));
      const additionalContacts = [patcontact];
      const expectedCollection: IContact[] = [...additionalContacts, ...contactCollection];
      jest.spyOn(contactService, 'addContactToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ participant });
      comp.ngOnInit();

      expect(contactService.query).toHaveBeenCalled();
      expect(contactService.addContactToCollectionIfMissing).toHaveBeenCalledWith(
        contactCollection,
        ...additionalContacts.map(expect.objectContaining),
      );
      expect(comp.contactsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Company query and add missing value', () => {
      const participant: IParticipant = { id: 456 };
      const patenquette: ICompany = { id: 21354 };
      participant.patenquette = patenquette;

      const companyCollection: ICompany[] = [{ id: 5017 }];
      jest.spyOn(companyService, 'query').mockReturnValue(of(new HttpResponse({ body: companyCollection })));
      const additionalCompanies = [patenquette];
      const expectedCollection: ICompany[] = [...additionalCompanies, ...companyCollection];
      jest.spyOn(companyService, 'addCompanyToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ participant });
      comp.ngOnInit();

      expect(companyService.query).toHaveBeenCalled();
      expect(companyService.addCompanyToCollectionIfMissing).toHaveBeenCalledWith(
        companyCollection,
        ...additionalCompanies.map(expect.objectContaining),
      );
      expect(comp.companiesSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const participant: IParticipant = { id: 456 };
      const patcontact: IContact = { id: 25903 };
      participant.patcontact = patcontact;
      const patenquette: ICompany = { id: 31900 };
      participant.patenquette = patenquette;

      activatedRoute.data = of({ participant });
      comp.ngOnInit();

      expect(comp.contactsSharedCollection).toContain(patcontact);
      expect(comp.companiesSharedCollection).toContain(patenquette);
      expect(comp.participant).toEqual(participant);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IParticipant>>();
      const participant = { id: 123 };
      jest.spyOn(participantFormService, 'getParticipant').mockReturnValue(participant);
      jest.spyOn(participantService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ participant });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: participant }));
      saveSubject.complete();

      // THEN
      expect(participantFormService.getParticipant).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(participantService.update).toHaveBeenCalledWith(expect.objectContaining(participant));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IParticipant>>();
      const participant = { id: 123 };
      jest.spyOn(participantFormService, 'getParticipant').mockReturnValue({ id: null });
      jest.spyOn(participantService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ participant: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: participant }));
      saveSubject.complete();

      // THEN
      expect(participantFormService.getParticipant).toHaveBeenCalled();
      expect(participantService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IParticipant>>();
      const participant = { id: 123 };
      jest.spyOn(participantService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ participant });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(participantService.update).toHaveBeenCalled();
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

    describe('compareCompany', () => {
      it('Should forward to companyService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(companyService, 'compareCompany');
        comp.compareCompany(entity, entity2);
        expect(companyService.compareCompany).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
