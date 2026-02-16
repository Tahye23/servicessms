import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IQuestion } from '../question.model';
import { QuestionService } from '../service/question.service';

const questionResolve = (route: ActivatedRouteSnapshot): Observable<null | IQuestion> => {
  const id = route.params['id'];
  if (id) {
    return inject(QuestionService)
      .find(id)
      .pipe(
        mergeMap((question: HttpResponse<IQuestion>) => {
          if (question.body) {
            return of(question.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default questionResolve;
