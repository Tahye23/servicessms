import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IParticipant } from '../participant.model';
import { ParticipantService } from '../service/participant.service';

const participantResolve = (route: ActivatedRouteSnapshot): Observable<null | IParticipant> => {
  const id = route.params['id'];
  if (id) {
    return inject(ParticipantService)
      .find(id)
      .pipe(
        mergeMap((participant: HttpResponse<IParticipant>) => {
          if (participant.body) {
            return of(participant.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default participantResolve;
