import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { OTPStorageService } from '../service/otp-storage.service';
import { IOTPStorage } from '../otp-storage.model';
import { OTPStorageFormService } from './otp-storage-form.service';

import { OTPStorageUpdateComponent } from './otp-storage-update.component';

describe('OTPStorage Management Update Component', () => {
  let comp: OTPStorageUpdateComponent;
  let fixture: ComponentFixture<OTPStorageUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let oTPStorageFormService: OTPStorageFormService;
  let oTPStorageService: OTPStorageService;
  let extendedUserService: ExtendedUserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, OTPStorageUpdateComponent],
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
      .overrideTemplate(OTPStorageUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(OTPStorageUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    oTPStorageFormService = TestBed.inject(OTPStorageFormService);
    oTPStorageService = TestBed.inject(OTPStorageService);
    extendedUserService = TestBed.inject(ExtendedUserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call ExtendedUser query and add missing value', () => {
      const oTPStorage: IOTPStorage = { id: 456 };
      const User: IExtendedUser = { id: 2348 };
      oTPStorage.User = User;

      const extendedUserCollection: IExtendedUser[] = [{ id: 5815 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [User];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ oTPStorage });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const oTPStorage: IOTPStorage = { id: 456 };
      const User: IExtendedUser = { id: 1008 };
      oTPStorage.User = User;

      activatedRoute.data = of({ oTPStorage });
      comp.ngOnInit();

      expect(comp.extendedUsersSharedCollection).toContain(User);
      expect(comp.oTPStorage).toEqual(oTPStorage);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IOTPStorage>>();
      const oTPStorage = { id: 123 };
      jest.spyOn(oTPStorageFormService, 'getOTPStorage').mockReturnValue(oTPStorage);
      jest.spyOn(oTPStorageService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ oTPStorage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: oTPStorage }));
      saveSubject.complete();

      // THEN
      expect(oTPStorageFormService.getOTPStorage).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(oTPStorageService.update).toHaveBeenCalledWith(expect.objectContaining(oTPStorage));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IOTPStorage>>();
      const oTPStorage = { id: 123 };
      jest.spyOn(oTPStorageFormService, 'getOTPStorage').mockReturnValue({ id: null });
      jest.spyOn(oTPStorageService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ oTPStorage: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: oTPStorage }));
      saveSubject.complete();

      // THEN
      expect(oTPStorageFormService.getOTPStorage).toHaveBeenCalled();
      expect(oTPStorageService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IOTPStorage>>();
      const oTPStorage = { id: 123 };
      jest.spyOn(oTPStorageService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ oTPStorage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(oTPStorageService.update).toHaveBeenCalled();
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
  });
});
