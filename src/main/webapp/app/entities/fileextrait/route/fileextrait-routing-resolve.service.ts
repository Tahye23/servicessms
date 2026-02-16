import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IFileextrait } from '../fileextrait.model';
import { FileextraitService } from '../service/fileextrait.service';

const fileextraitResolve = (route: ActivatedRouteSnapshot): Observable<null | IFileextrait> => {
  const id = route.params['id'];
  if (id) {
    return inject(FileextraitService)
      .find(id)
      .pipe(
        mergeMap((fileextrait: HttpResponse<IFileextrait>) => {
          if (fileextrait.body) {
            return of(fileextrait.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default fileextraitResolve;
