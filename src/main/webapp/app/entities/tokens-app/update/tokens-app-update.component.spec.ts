import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { IApplication } from 'app/entities/application/application.model';
import { ApplicationService } from 'app/entities/application/service/application.service';
import { TokensAppService } from '../service/tokens-app.service';
import { ITokensApp } from '../tokens-app.model';
import { TokensAppFormService } from './tokens-app-form.service';

import { TokensAppUpdateComponent } from './tokens-app-update.component';

describe('TokensApp Management Update Component', () => {
  let comp: TokensAppUpdateComponent;
  let fixture: ComponentFixture<TokensAppUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let tokensAppFormService: TokensAppFormService;
  let tokensAppService: TokensAppService;
  let applicationService: ApplicationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, TokensAppUpdateComponent],
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
      .overrideTemplate(TokensAppUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(TokensAppUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    tokensAppFormService = TestBed.inject(TokensAppFormService);
    tokensAppService = TestBed.inject(TokensAppService);
    applicationService = TestBed.inject(ApplicationService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Application query and add missing value', () => {
      const tokensApp: ITokensApp = { id: 456 };
      const application: IApplication = { id: 31296 };
      tokensApp.application = application;

      const applicationCollection: IApplication[] = [{ id: 25104 }];
      jest.spyOn(applicationService, 'query').mockReturnValue(of(new HttpResponse({ body: applicationCollection })));
      const additionalApplications = [application];
      const expectedCollection: IApplication[] = [...additionalApplications, ...applicationCollection];
      jest.spyOn(applicationService, 'addApplicationToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ tokensApp });
      comp.ngOnInit();

      expect(applicationService.query).toHaveBeenCalled();
      expect(applicationService.addApplicationToCollectionIfMissing).toHaveBeenCalledWith(
        applicationCollection,
        ...additionalApplications.map(expect.objectContaining),
      );
      expect(comp.applicationsSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const tokensApp: ITokensApp = { id: 456 };
      const application: IApplication = { id: 583 };
      tokensApp.application = application;

      activatedRoute.data = of({ tokensApp });
      comp.ngOnInit();

      expect(comp.applicationsSharedCollection).toContain(application);
      expect(comp.tokensApp).toEqual(tokensApp);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ITokensApp>>();
      const tokensApp = { id: 123 };
      jest.spyOn(tokensAppFormService, 'getTokensApp').mockReturnValue(tokensApp);
      jest.spyOn(tokensAppService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tokensApp });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: tokensApp }));
      saveSubject.complete();

      // THEN
      expect(tokensAppFormService.getTokensApp).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(tokensAppService.update).toHaveBeenCalledWith(expect.objectContaining(tokensApp));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ITokensApp>>();
      const tokensApp = { id: 123 };
      jest.spyOn(tokensAppFormService, 'getTokensApp').mockReturnValue({ id: null });
      jest.spyOn(tokensAppService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tokensApp: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: tokensApp }));
      saveSubject.complete();

      // THEN
      expect(tokensAppFormService.getTokensApp).toHaveBeenCalled();
      expect(tokensAppService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<ITokensApp>>();
      const tokensApp = { id: 123 };
      jest.spyOn(tokensAppService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tokensApp });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(tokensAppService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareApplication', () => {
      it('Should forward to applicationService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(applicationService, 'compareApplication');
        comp.compareApplication(entity, entity2);
        expect(applicationService.compareApplication).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
