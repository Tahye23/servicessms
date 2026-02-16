import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { ITokensApp } from '../tokens-app.model';
import { TokensAppService } from '../service/tokens-app.service';

const tokensAppResolve = (route: ActivatedRouteSnapshot): Observable<null | ITokensApp> => {
  const id = route.params['id'];
  if (id) {
    return inject(TokensAppService)
      .find(id)
      .pipe(
        mergeMap((tokensApp: HttpResponse<ITokensApp>) => {
          if (tokensApp.body) {
            return of(tokensApp.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default tokensAppResolve;
