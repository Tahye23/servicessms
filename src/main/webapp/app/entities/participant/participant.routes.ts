import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ParticipantComponent } from './list/participant.component';
import { ParticipantDetailComponent } from './detail/participant-detail.component';
import { ParticipantUpdateComponent } from './update/participant-update.component';
import ParticipantResolve from './route/participant-routing-resolve.service';

const participantRoute: Routes = [
  {
    path: '',
    component: ParticipantComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ParticipantDetailComponent,
    resolve: {
      participant: ParticipantResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ParticipantUpdateComponent,
    resolve: {
      participant: ParticipantResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ParticipantUpdateComponent,
    resolve: {
      participant: ParticipantResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default participantRoute;
