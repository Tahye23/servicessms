import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { PlanabonnementDetailComponent } from './planabonnement-detail.component';

describe('Planabonnement Management Detail Component', () => {
  let comp: PlanabonnementDetailComponent;
  let fixture: ComponentFixture<PlanabonnementDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanabonnementDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: PlanabonnementDetailComponent,
              resolve: { planabonnement: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(PlanabonnementDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PlanabonnementDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load planabonnement on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', PlanabonnementDetailComponent);

      // THEN
      expect(instance.planabonnement()).toEqual(expect.objectContaining({ id: 123 }));
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
