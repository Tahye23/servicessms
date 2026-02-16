import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { QuestionDetailComponent } from './question-detail.component';

describe('Question Management Detail Component', () => {
  let comp: QuestionDetailComponent;
  let fixture: ComponentFixture<QuestionDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuestionDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: QuestionDetailComponent,
              resolve: { question: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(QuestionDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(QuestionDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load question on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', QuestionDetailComponent);

      // THEN
      expect(instance.question()).toEqual(expect.objectContaining({ id: 123 }));
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
