import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { TokensAppDetailComponent } from './tokens-app-detail.component';

describe('TokensApp Management Detail Component', () => {
  let comp: TokensAppDetailComponent;
  let fixture: ComponentFixture<TokensAppDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TokensAppDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: TokensAppDetailComponent,
              resolve: { tokensApp: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(TokensAppDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TokensAppDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load tokensApp on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', TokensAppDetailComponent);

      // THEN
      expect(instance.tokensApp()).toEqual(expect.objectContaining({ id: 123 }));
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
