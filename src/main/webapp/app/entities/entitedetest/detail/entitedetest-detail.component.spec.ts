import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { EntitedetestDetailComponent } from './entitedetest-detail.component';

describe('Entitedetest Management Detail Component', () => {
  let comp: EntitedetestDetailComponent;
  let fixture: ComponentFixture<EntitedetestDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EntitedetestDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: EntitedetestDetailComponent,
              resolve: { entitedetest: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(EntitedetestDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EntitedetestDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load entitedetest on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', EntitedetestDetailComponent);

      // THEN
      expect(instance.entitedetest()).toEqual(expect.objectContaining({ id: 123 }));
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
