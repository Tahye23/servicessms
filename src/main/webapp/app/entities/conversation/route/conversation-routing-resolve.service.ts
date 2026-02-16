import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { of, EMPTY, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

import { IConversation } from '../conversation.model';
import { ConversationService } from '../service/conversation.service';

const conversationResolve = (route: ActivatedRouteSnapshot): Observable<null | IConversation> => {
  const id = route.params['id'];
  if (id) {
    return inject(ConversationService)
      .find(id)
      .pipe(
        mergeMap((conversation: HttpResponse<IConversation>) => {
          if (conversation.body) {
            return of(conversation.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(null);
};

export default conversationResolve;
