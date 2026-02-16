import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IGroupe } from 'app/entities/groupe/groupe.model';
import { GroupeService } from 'app/entities/groupe/service/groupe.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IGroupedecontact } from '../groupedecontact.model';
import { GroupedecontactService } from '../service/groupedecontact.service';
import { GroupedecontactFormService } from './groupedecontact-form.service';

import { GroupedecontactUpdateComponent } from './groupedecontact-update.component';

describe('Groupedecontact Management Update Component', () => {
  let comp: GroupedecontactUpdateComponent;
  let fixture: ComponentFixture<GroupedecontactUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let groupedecontactFormService: GroupedecontactFormService;
  let groupedecontactService: GroupedecontactService;
  let groupeService: GroupeService;
  let contactService: ContactService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, GroupedecontactUpdateComponent],
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
      .overrideTemplate(GroupedecontactUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(GroupedecontactUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    groupedecontactFormService = TestBed.inject(GroupedecontactFormService);
    groupedecontactService = TestBed.inject(GroupedecontactService);
    groupeService = TestBed.inject(GroupeService);
    contactService = TestBed.inject(ContactService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Groupe query and add missing value', () => {
      const groupedecontact: IGroupedecontact = { id: 456 };
      const cgrgroupe: IGroupe = { id: 32433 };
      groupedecontact.cgrgroupe = cgrgroupe;

      const groupeCollection: IGroupe[] = [{ id: 22188 }];
      jest.spyOn(groupeService, 'query').mockReturnValue(of(new HttpResponse({ body: groupeCollection })));
      const additionalGroupes = [cgrgroupe];
      const expectedCollection: IGroupe[] = [...additionalGroupes, ...groupeCollection];
      jest.spyOn(groupeService, 'addGroupeToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupedecontact });
      comp.ngOnInit();

      expect(groupeService.query).toHaveBeenCalled();
      expect(groupeService.addGroupeToCollectionIfMissing).toHaveBeenCalledWith(
        groupeCollection,
        ...additionalGroupes.map(expect.objectContaining),
      );
      expect(comp.groupesSharedCollection).toEqual(expectedCollection);
    });

    it('Should call Contact query and add missing value', () => {
      const groupedecontact: IGroupedecontact = { id: 456 };
      const contact: IContact = { id: 31541 };
      groupedecontact.contact = contact;

      const contactCollection: IContact[] = [{ id: 18813 }];
      jest.spyOn(contactService, 'query').mockReturnValue(of(new HttpResponse({ body: contactCollection })));
      const additionalContacts = [contact];
      const expectedCollection: IContact[] = [...additionalContacts, ...contactCollection];
      jest.spyOn(contactService, 'addContactToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupedecontact });
      comp.ngOnInit();

      expect(contactService.query).toHaveBeenCalled();
      expect(contactService.addContactToCollectionIfMissing).toHaveBeenCalledWith(
        contactCollection,
        ...additionalContacts.map(expect.objectContaining),
      );
      expect(comp.contactsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const groupedecontact: IGroupedecontact = { id: 456 };
      const cgrgroupe: IGroupe = { id: 13082 };
      groupedecontact.cgrgroupe = cgrgroupe;
      const contact: IContact = { id: 21779 };
      groupedecontact.contact = contact;

      activatedRoute.data = of({ groupedecontact });
      comp.ngOnInit();

      expect(comp.groupesSharedCollection).toContain(cgrgroupe);
      expect(comp.contactsSharedCollection).toContain(contact);
      expect(comp.groupedecontact).toEqual(groupedecontact);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupedecontact>>();
      const groupedecontact = { id: 123 };
      jest.spyOn(groupedecontactFormService, 'getGroupedecontact').mockReturnValue(groupedecontact);
      jest.spyOn(groupedecontactService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupedecontact });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupedecontact }));
      saveSubject.complete();

      // THEN
      expect(groupedecontactFormService.getGroupedecontact).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(groupedecontactService.update).toHaveBeenCalledWith(expect.objectContaining(groupedecontact));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupedecontact>>();
      const groupedecontact = { id: 123 };
      jest.spyOn(groupedecontactFormService, 'getGroupedecontact').mockReturnValue({ id: null });
      jest.spyOn(groupedecontactService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupedecontact: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupedecontact }));
      saveSubject.complete();

      // THEN
      expect(groupedecontactFormService.getGroupedecontact).toHaveBeenCalled();
      expect(groupedecontactService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupedecontact>>();
      const groupedecontact = { id: 123 };
      jest.spyOn(groupedecontactService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupedecontact });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(groupedecontactService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareGroupe', () => {
      it('Should forward to groupeService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(groupeService, 'compareGroupe');
        comp.compareGroupe(entity, entity2);
        expect(groupeService.compareGroupe).toHaveBeenCalledWith(entity, entity2);
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
