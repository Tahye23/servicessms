import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ConversationDetailComponent } from './conversation-detail.component';

describe('Conversation Management Detail Component', () => {
  let comp: ConversationDetailComponent;
  let fixture: ComponentFixture<ConversationDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConversationDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ConversationDetailComponent,
              resolve: { conversation: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ConversationDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ConversationDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load conversation on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ConversationDetailComponent);

      // THEN
      expect(instance.conversation()).toEqual(expect.objectContaining({ id: 123 }));
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
