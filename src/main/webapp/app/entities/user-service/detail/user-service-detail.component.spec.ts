import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { UserServiceDetailComponent } from './user-service-detail.component';

describe('UserService Management Detail Component', () => {
  let comp: UserServiceDetailComponent;
  let fixture: ComponentFixture<UserServiceDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserServiceDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: UserServiceDetailComponent,
              resolve: { userService: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(UserServiceDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UserServiceDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load userService on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', UserServiceDetailComponent);

      // THEN
      expect(instance.userService()).toEqual(expect.objectContaining({ id: 123 }));
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
