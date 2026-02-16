import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { AbonnementDetailComponent } from './abonnement-detail.component';

describe('Abonnement Management Detail Component', () => {
  let comp: AbonnementDetailComponent;
  let fixture: ComponentFixture<AbonnementDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AbonnementDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: AbonnementDetailComponent,
              resolve: { abonnement: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(AbonnementDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AbonnementDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load abonnement on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', AbonnementDetailComponent);

      // THEN
      expect(instance.abonnement()).toEqual(expect.objectContaining({ id: 123 }));
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
