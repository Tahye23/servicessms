import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { GroupedecontactDetailComponent } from './groupedecontact-detail.component';

describe('Groupedecontact Management Detail Component', () => {
  let comp: GroupedecontactDetailComponent;
  let fixture: ComponentFixture<GroupedecontactDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupedecontactDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: GroupedecontactDetailComponent,
              resolve: { groupedecontact: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(GroupedecontactDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupedecontactDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load groupedecontact on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', GroupedecontactDetailComponent);

      // THEN
      expect(instance.groupedecontact()).toEqual(expect.objectContaining({ id: 123 }));
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
