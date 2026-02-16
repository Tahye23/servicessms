import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../tokens-app.test-samples';

import { TokensAppFormService } from './tokens-app-form.service';

describe('TokensApp Form Service', () => {
  let service: TokensAppFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokensAppFormService);
  });

  describe('Service methods', () => {
    describe('createTokensAppFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createTokensAppFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            dateExpiration: expect.any(Object),
            token: expect.any(Object),
            application: expect.any(Object),
          }),
        );
      });

      it('passing ITokensApp should create a new form with FormGroup', () => {
        const formGroup = service.createTokensAppFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            dateExpiration: expect.any(Object),
            token: expect.any(Object),
            application: expect.any(Object),
          }),
        );
      });
    });

    describe('getTokensApp', () => {
      it('should return NewTokensApp for default TokensApp initial value', () => {
        const formGroup = service.createTokensAppFormGroup(sampleWithNewData);

        const tokensApp = service.getTokensApp(formGroup) as any;

        expect(tokensApp).toMatchObject(sampleWithNewData);
      });

      it('should return NewTokensApp for empty TokensApp initial value', () => {
        const formGroup = service.createTokensAppFormGroup();

        const tokensApp = service.getTokensApp(formGroup) as any;

        expect(tokensApp).toMatchObject({});
      });

      it('should return ITokensApp', () => {
        const formGroup = service.createTokensAppFormGroup(sampleWithRequiredData);

        const tokensApp = service.getTokensApp(formGroup) as any;

        expect(tokensApp).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing ITokensApp should not enable id FormControl', () => {
        const formGroup = service.createTokensAppFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewTokensApp should disable id FormControl', () => {
        const formGroup = service.createTokensAppFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
