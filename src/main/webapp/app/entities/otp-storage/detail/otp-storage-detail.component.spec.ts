import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { OTPStorageDetailComponent } from './otp-storage-detail.component';

describe('OTPStorage Management Detail Component', () => {
  let comp: OTPStorageDetailComponent;
  let fixture: ComponentFixture<OTPStorageDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OTPStorageDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: OTPStorageDetailComponent,
              resolve: { oTPStorage: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(OTPStorageDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OTPStorageDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load oTPStorage on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', OTPStorageDetailComponent);

      // THEN
      expect(instance.oTPStorage()).toEqual(expect.objectContaining({ id: 123 }));
    });
  });

  describe('PreviousState', () => {
    it('Should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
