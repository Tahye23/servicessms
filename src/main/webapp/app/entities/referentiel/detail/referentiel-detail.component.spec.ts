import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ReferentielDetailComponent } from './referentiel-detail.component';

describe('Referentiel Management Detail Component', () => {
  let comp: ReferentielDetailComponent;
  let fixture: ComponentFixture<ReferentielDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReferentielDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ReferentielDetailComponent,
              resolve: { referentiel: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ReferentielDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ReferentielDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load referentiel on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ReferentielDetailComponent);

      // THEN
      expect(instance.referentiel()).toEqual(expect.objectContaining({ id: 123 }));
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
