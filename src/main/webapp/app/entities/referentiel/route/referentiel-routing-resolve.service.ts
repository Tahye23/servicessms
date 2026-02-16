import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IReferentiel } from '../referentiel.model';
import { ReferentielService } from '../service/referentiel.service';

const referentielResolve = (route: ActivatedRouteSnapshot): Observable<null | IReferentiel> => {
  const id = route.params['id'];
  if (id) {
    return inject(ReferentielService)
      .find(id)
      .pipe(
        mergeMap((referentiel: HttpResponse<IReferentiel>) => {
          if (referentiel.body) {
            return of(referentiel.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default referentielResolve;
