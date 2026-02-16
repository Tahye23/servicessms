import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../fileextrait.test-samples';

import { FileextraitFormService } from './fileextrait-form.service';

describe('Fileextrait Form Service', () => {
  let service: FileextraitFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FileextraitFormService);
  });

  describe('Service methods', () => {
    describe('createFileextraitFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createFileextraitFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fexidfile: expect.any(Object),
            fexparent: expect.any(Object),
            fexdata: expect.any(Object),
            fextype: expect.any(Object),
            fexname: expect.any(Object),
          }),
        );
      });

      it('passing IFileextrait should create a new form with FormGroup', () => {
        const formGroup = service.createFileextraitFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            fexidfile: expect.any(Object),
            fexparent: expect.any(Object),
            fexdata: expect.any(Object),
            fextype: expect.any(Object),
            fexname: expect.any(Object),
          }),
        );
      });
    });

    describe('getFileextrait', () => {
      it('should return NewFileextrait for default Fileextrait initial value', () => {
        const formGroup = service.createFileextraitFormGroup(sampleWithNewData);

        const fileextrait = service.getFileextrait(formGroup) as any;

        expect(fileextrait).toMatchObject(sampleWithNewData);
      });

      it('should return NewFileextrait for empty Fileextrait initial value', () => {
        const formGroup = service.createFileextraitFormGroup();

        const fileextrait = service.getFileextrait(formGroup) as any;

        expect(fileextrait).toMatchObject({});
      });

      it('should return IFileextrait', () => {
        const formGroup = service.createFileextraitFormGroup(sampleWithRequiredData);

        const fileextrait = service.getFileextrait(formGroup) as any;

        expect(fileextrait).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IFileextrait should not enable id FormControl', () => {
        const formGroup = service.createFileextraitFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewFileextrait should disable id FormControl', () => {
        const formGroup = service.createFileextraitFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
