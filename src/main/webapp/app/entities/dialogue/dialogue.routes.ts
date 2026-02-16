import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { DialogueComponent } from './list/dialogue.component';
import { DialogueDetailComponent } from './detail/dialogue-detail.component';
import { DialogueUpdateComponent } from './update/dialogue-update.component';
import DialogueResolve from './route/dialogue-routing-resolve.service';

const dialogueRoute: Routes = [
  {
    path: '',
    component: DialogueComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: DialogueDetailComponent,
    resolve: {
      dialogue: DialogueResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: DialogueUpdateComponent,
    resolve: {
      dialogue: DialogueResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: DialogueUpdateComponent,
    resolve: {
      dialogue: DialogueResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default dialogueRoute;
