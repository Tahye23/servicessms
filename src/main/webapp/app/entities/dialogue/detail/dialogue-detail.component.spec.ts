import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { DialogueDetailComponent } from './dialogue-detail.component';

describe('Dialogue Management Detail Component', () => {
  let comp: DialogueDetailComponent;
  let fixture: ComponentFixture<DialogueDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DialogueDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: DialogueDetailComponent,
              resolve: { dialogue: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(DialogueDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogueDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load dialogue on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', DialogueDetailComponent);

      // THEN
      expect(instance.dialogue()).toEqual(expect.objectContaining({ id: 123 }));
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
