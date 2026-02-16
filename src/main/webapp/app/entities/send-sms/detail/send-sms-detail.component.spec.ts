import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { SendSmsDetailComponent } from './send-sms-detail.component';

describe('SendSms Management Detail Component', () => {
  let comp: SendSmsDetailComponent;
  let fixture: ComponentFixture<SendSmsDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SendSmsDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: SendSmsDetailComponent,
              resolve: { sendSms: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(SendSmsDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SendSmsDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load sendSms on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', SendSmsDetailComponent);

      // THEN
      expect(instance.sendSms()).toEqual(expect.objectContaining({ id: 123 }));
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
