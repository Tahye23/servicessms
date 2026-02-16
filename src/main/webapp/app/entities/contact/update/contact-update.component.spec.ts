import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IGroupe } from 'app/entities/groupe/groupe.model';
import { GroupeService } from 'app/entities/groupe/service/groupe.service';
import { ContactService } from '../service/contact.service';
import { IContact } from '../contact.model';
import { ContactFormService } from './contact-form.service';

import { ContactUpdateComponent } from './contact-update.component';

describe('Contact Management Update Component', () => {
  let comp: ContactUpdateComponent;
  let fixture: ComponentFixture<ContactUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let contactFormService: ContactFormService;
  let contactService: ContactService;
  let groupeService: GroupeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, ContactUpdateComponent],
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
      .overrideTemplate(ContactUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ContactUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    contactFormService = TestBed.inject(ContactFormService);
    contactService = TestBed.inject(ContactService);
    groupeService = TestBed.inject(GroupeService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Groupe query and add missing value', () => {
      const contact: IContact = { id: 456 };
      const Groupe: IGroupe = { id: 30745 };
      contact.Groupe = Groupe;

      const groupeCollection: IGroupe[] = [{ id: 7518 }];
      jest.spyOn(groupeService, 'query').mockReturnValue(of(new HttpResponse({ body: groupeCollection })));
      const additionalGroupes = [Groupe];
      const expectedCollection: IGroupe[] = [...additionalGroupes, ...groupeCollection];
      jest.spyOn(groupeService, 'addGroupeToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ contact });
      comp.ngOnInit();

      expect(groupeService.query).toHaveBeenCalled();
      expect(groupeService.addGroupeToCollectionIfMissing).toHaveBeenCalledWith(
        groupeCollection,
        ...additionalGroupes.map(expect.objectContaining),
      );
      expect(comp.groupesSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const contact: IContact = { id: 456 };
      const Groupe: IGroupe = { id: 24397 };
      contact.Groupe = Groupe;

      activatedRoute.data = of({ contact });
      comp.ngOnInit();

      expect(comp.groupesSharedCollection).toContain(Groupe);
      expect(comp.contact).toEqual(contact);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IContact>>();
      const contact = { id: 123 };
      jest.spyOn(contactFormService, 'getContact').mockReturnValue(contact);
      jest.spyOn(contactService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ contact });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: contact }));
      saveSubject.complete();

      // THEN
      expect(contactFormService.getContact).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(contactService.update).toHaveBeenCalledWith(expect.objectContaining(contact));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IContact>>();
      const contact = { id: 123 };
      jest.spyOn(contactFormService, 'getContact').mockReturnValue({ id: null });
      jest.spyOn(contactService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ contact: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: contact }));
      saveSubject.complete();

      // THEN
      expect(contactFormService.getContact).toHaveBeenCalled();
      expect(contactService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IContact>>();
      const contact = { id: 123 };
      jest.spyOn(contactService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ contact });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(contactService.update).toHaveBeenCalled();
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
  });
});
