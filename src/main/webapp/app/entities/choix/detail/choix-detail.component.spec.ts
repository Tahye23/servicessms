import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ChoixDetailComponent } from './choix-detail.component';

describe('Choix Management Detail Component', () => {
  let comp: ChoixDetailComponent;
  let fixture: ComponentFixture<ChoixDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChoixDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ChoixDetailComponent,
              resolve: { choix: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ChoixDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ChoixDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load choix on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ChoixDetailComponent);

      // THEN
      expect(instance.choix()).toEqual(expect.objectContaining({ id: 123 }));
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
