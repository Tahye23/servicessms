import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { UserTokenApiDetailComponent } from './user-token-api-detail.component';

describe('UserTokenApi Management Detail Component', () => {
  let comp: UserTokenApiDetailComponent;
  let fixture: ComponentFixture<UserTokenApiDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserTokenApiDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: UserTokenApiDetailComponent,
              resolve: { userTokenApi: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(UserTokenApiDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UserTokenApiDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load userTokenApi on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', UserTokenApiDetailComponent);

      // THEN
      expect(instance.userTokenApi()).toEqual(expect.objectContaining({ id: 123 }));
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
