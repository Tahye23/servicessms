import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IGroupe } from '../groupe.model';
import { GroupeService } from '../service/groupe.service';

const groupeResolve = (route: ActivatedRouteSnapshot): Observable<null | IGroupe> => {
  const id = route.params['id'];
  if (id) {
    return inject(GroupeService)
      .find(id)
      .pipe(
        mergeMap((groupe: HttpResponse<IGroupe>) => {
          if (groupe.body) {
            return of(groupe.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default groupeResolve;
