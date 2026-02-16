import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { of, Subject, from } from 'rxjs';

import { DialogueService } from '../service/dialogue.service';
import { IDialogue } from '../dialogue.model';
import { DialogueFormService } from './dialogue-form.service';

import { DialogueUpdateComponent } from './dialogue-update.component';

describe('Dialogue Management Update Component', () => {
  let comp: DialogueUpdateComponent;
  let fixture: ComponentFixture<DialogueUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let dialogueFormService: DialogueFormService;
  let dialogueService: DialogueService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, DialogueUpdateComponent],
      providers: [
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(DialogueUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(DialogueUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    dialogueFormService = TestBed.inject(DialogueFormService);
    dialogueService = TestBed.inject(DialogueService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const dialogue: IDialogue = { id: 456 };

      activatedRoute.data = of({ dialogue });
      comp.ngOnInit();

      expect(comp.dialogue).toEqual(dialogue);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IDialogue>>();
      const dialogue = { id: 123 };
      jest.spyOn(dialogueFormService, 'getDialogue').mockReturnValue(dialogue);
      jest.spyOn(dialogueService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ dialogue });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: dialogue }));
      saveSubject.complete();

      // THEN
      expect(dialogueFormService.getDialogue).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(dialogueService.update).toHaveBeenCalledWith(expect.objectContaining(dialogue));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IDialogue>>();
      const dialogue = { id: 123 };
      jest.spyOn(dialogueFormService, 'getDialogue').mockReturnValue({ id: null });
      jest.spyOn(dialogueService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ dialogue: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: dialogue }));
      saveSubject.complete();

      // THEN
      expect(dialogueFormService.getDialogue).toHaveBeenCalled();
      expect(dialogueService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IDialogue>>();
      const dialogue = { id: 123 };
      jest.spyOn(dialogueService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ dialogue });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(dialogueService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
