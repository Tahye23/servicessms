import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { ConversationComponent } from './list/conversation.component';
import { ConversationDetailComponent } from './detail/conversation-detail.component';
import { ConversationUpdateComponent } from './update/conversation-update.component';
import ConversationResolve from './route/conversation-routing-resolve.service';

const conversationRoute: Routes = [
  {
    path: '',
    component: ConversationComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ConversationDetailComponent,
    resolve: {
      conversation: ConversationResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ConversationUpdateComponent,
    resolve: {
      conversation: ConversationResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ConversationUpdateComponent,
    resolve: {
      conversation: ConversationResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default conversationRoute;
