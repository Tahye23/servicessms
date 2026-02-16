import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IDialogue } from '../dialogue.model';
import { DialogueService } from '../service/dialogue.service';

const dialogueResolve = (route: ActivatedRouteSnapshot): Observable<null | IDialogue> => {
  const id = route.params['id'];
  if (id) {
    return inject(DialogueService)
      .find(id)
      .pipe(
        mergeMap((dialogue: HttpResponse<IDialogue>) => {
          if (dialogue.body) {
            return of(dialogue.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default dialogueResolve;
