import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ReponseDetailComponent } from './reponse-detail.component';

describe('Reponse Management Detail Component', () => {
  let comp: ReponseDetailComponent;
  let fixture: ComponentFixture<ReponseDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReponseDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ReponseDetailComponent,
              resolve: { reponse: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ReponseDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ReponseDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load reponse on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ReponseDetailComponent);

      // THEN
      expect(instance.reponse()).toEqual(expect.objectContaining({ id: 123 }));
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
