import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IApi } from 'app/entities/api/api.model';
import { ApiService } from 'app/entities/api/service/api.service';
import { ITokensApp } from 'app/entities/tokens-app/tokens-app.model';
import { TokensAppService } from 'app/entities/tokens-app/service/tokens-app.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IUserTokenApi } from '../user-token-api.model';
import { UserTokenApiService } from '../service/user-token-api.service';
import { UserTokenApiFormService } from './user-token-api-form.service';

import { UserTokenApiUpdateComponent } from './user-token-api-update.component';

describe('UserTokenApi Management Update Component', () => {
  let comp: UserTokenApiUpdateComponent;
  let fixture: ComponentFixture<UserTokenApiUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let userTokenApiFormService: UserTokenApiFormService;
  let userTokenApiService: UserTokenApiService;
  let apiService: ApiService;
  let tokensAppService: TokensAppService;
  let extendedUserService: ExtendedUserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, UserTokenApiUpdateComponent],
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
      .overrideTemplate(UserTokenApiUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(UserTokenApiUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    userTokenApiFormService = TestBed.inject(UserTokenApiFormService);
    userTokenApiService = TestBed.inject(UserTokenApiService);
    apiService = TestBed.inject(ApiService);
    tokensAppService = TestBed.inject(TokensAppService);
    extendedUserService = TestBed.inject(ExtendedUserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Api query and add missing value', () => {
      const userTokenApi: IUserTokenApi = { id: 456 };
      const api: IApi = { id: 8089 };
      userTokenApi.api = api;

      const apiCollection: IApi[] = [{ id: 12132 }];
      jest.spyOn(apiService, 'query').mockReturnValue(of(new HttpResponse({ body: apiCollection })));
      const additionalApis = [api];
      const expectedCollection: IApi[] = [...additionalApis, ...apiCollection];
      jest.spyOn(apiService, 'addApiToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      expect(apiService.query).toHaveBeenCalled();
      expect(apiService.addApiToCollectionIfMissing).toHaveBeenCalledWith(apiCollection, ...additionalApis.map(expect.objectContaining));
      expect(comp.apisSharedCollection).toEqual(expectedCollection);
    });

    it('Should call TokensApp query and add missing value', () => {
      const userTokenApi: IUserTokenApi = { id: 456 };
      const token: ITokensApp = { id: 828 };
      userTokenApi.token = token;

      const tokensAppCollection: ITokensApp[] = [{ id: 30421 }];
      jest.spyOn(tokensAppService, 'query').mockReturnValue(of(new HttpResponse({ body: tokensAppCollection })));
      const additionalTokensApps = [token];
      const expectedCollection: ITokensApp[] = [...additionalTokensApps, ...tokensAppCollection];
      jest.spyOn(tokensAppService, 'addTokensAppToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      expect(tokensAppService.query).toHaveBeenCalled();
      expect(tokensAppService.addTokensAppToCollectionIfMissing).toHaveBeenCalledWith(
        tokensAppCollection,
        ...additionalTokensApps.map(expect.objectContaining),
      );
      expect(comp.tokensAppsSharedCollection).toEqual(expectedCollection);
    });

    it('Should call ExtendedUser query and add missing value', () => {
      const userTokenApi: IUserTokenApi = { id: 456 };
      const user: IExtendedUser = { id: 25406 };
      userTokenApi.user = user;

      const extendedUserCollection: IExtendedUser[] = [{ id: 333 }];
      jest.spyOn(extendedUserService, 'query').mockReturnValue(of(new HttpResponse({ body: extendedUserCollection })));
      const additionalExtendedUsers = [user];
      const expectedCollection: IExtendedUser[] = [...additionalExtendedUsers, ...extendedUserCollection];
      jest.spyOn(extendedUserService, 'addExtendedUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      expect(extendedUserService.query).toHaveBeenCalled();
      expect(extendedUserService.addExtendedUserToCollectionIfMissing).toHaveBeenCalledWith(
        extendedUserCollection,
        ...additionalExtendedUsers.map(expect.objectContaining),
      );
      expect(comp.extendedUsersSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const userTokenApi: IUserTokenApi = { id: 456 };
      const api: IApi = { id: 28290 };
      userTokenApi.api = api;
      const token: ITokensApp = { id: 16810 };
      userTokenApi.token = token;
      const user: IExtendedUser = { id: 24923 };
      userTokenApi.user = user;

      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      expect(comp.apisSharedCollection).toContain(api);
      expect(comp.tokensAppsSharedCollection).toContain(token);
      expect(comp.extendedUsersSharedCollection).toContain(user);
      expect(comp.userTokenApi).toEqual(userTokenApi);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserTokenApi>>();
      const userTokenApi = { id: 123 };
      jest.spyOn(userTokenApiFormService, 'getUserTokenApi').mockReturnValue(userTokenApi);
      jest.spyOn(userTokenApiService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: userTokenApi }));
      saveSubject.complete();

      // THEN
      expect(userTokenApiFormService.getUserTokenApi).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(userTokenApiService.update).toHaveBeenCalledWith(expect.objectContaining(userTokenApi));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserTokenApi>>();
      const userTokenApi = { id: 123 };
      jest.spyOn(userTokenApiFormService, 'getUserTokenApi').mockReturnValue({ id: null });
      jest.spyOn(userTokenApiService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userTokenApi: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: userTokenApi }));
      saveSubject.complete();

      // THEN
      expect(userTokenApiFormService.getUserTokenApi).toHaveBeenCalled();
      expect(userTokenApiService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IUserTokenApi>>();
      const userTokenApi = { id: 123 };
      jest.spyOn(userTokenApiService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userTokenApi });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(userTokenApiService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareApi', () => {
      it('Should forward to apiService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(apiService, 'compareApi');
        comp.compareApi(entity, entity2);
        expect(apiService.compareApi).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareTokensApp', () => {
      it('Should forward to tokensAppService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(tokensAppService, 'compareTokensApp');
        comp.compareTokensApp(entity, entity2);
        expect(tokensAppService.compareTokensApp).toHaveBeenCalledWith(entity, entity2);
      });
    });

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
