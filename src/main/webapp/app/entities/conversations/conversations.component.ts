import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy, NgZone, ChangeDetectorRef, HostListener } from '@angular/core';
import { NgIf, NgForOf, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IContact } from '../contact/contact.model';
import { NgForm } from '@angular/forms';
import { MessageType, Sms } from '../send-sms/send-sms.model';
import { ContactService } from '../contact/service/contact.service';
import { TemplateRendererComponent } from '../template/detailMessage/template-renderer.component';
import { ScrollingModule, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { BehaviorSubject, Subject, debounceTime, distinctUntilChanged, fromEvent, takeUntil } from 'rxjs';
import { Chat, GroupedChats } from '../chat/chat.model';
import { ChatService } from '../chat/service/chat.service';
import { HttpResponse } from '@angular/common/http';
import { SmsService } from '../sms/sms.service';
import { TemplateService } from '../template/service/template.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-conversations',
  standalone: true,
  imports: [
    NgIf,
    NgForOf,
    DatePipe,
    FormsModule,
    TemplateRendererComponent,
    ScrollingModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
  ],
  templateUrl: './conversations.component.html',
  styleUrl: './conversations.component.css',
})
export class ConversationsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef<HTMLElement>;
  @ViewChild('messagesViewport') messagesViewport!: CdkVirtualScrollViewport;
  @ViewChild('contactsContainer') contactsContainer!: ElementRef<HTMLElement>;

  // Chats management
  chats: Chat[] = [];
  groupedChats: GroupedChats = {};
  groupedChatsArray: Array<{ contact: IContact; chats: Chat[] }> = [];
  combine: Array<{ contact: IContact; chats: Chat[] }> = [];
  filteredGroupedChats: Array<{ contact: IContact; chats: Chat[] }> = [];
  totalChats = 0;
  page = 0;
  itemsPerPage = 20;
  totalPages = 0;
  totalItems = 0;
  loadingChats = false;
  searchTerm = '';
  searchSubject = new BehaviorSubject<string>('');

  // Selected contact & messages
  selectedContact: IContact | null = null;
  selectedChat?: Chat;
  selectedChatId!: number;
  messages: Sms[] = [];
  newMessage = '';
  sending = false;

  selectedChannel?: 'WHATSAPP' | 'SMS';

  // Message pagination
  messagesPage = 0;
  messagesPerPage = 30;
  totalMessages = 0;
  noMoreMessages = false;
  loadingMessages = false;
  loadingMoreMessages = false;

  // UI controls
  showScrollButton = false;
  isScrolling = false;
  scrollTimeout: any;
  availableChannels: string[] = [];

  // Date grouping cache
  messageDates = new Map<number, Date>();

  // Cleanup
  private destroy$ = new Subject<void>();

  formData = {
    contactId: '',
    channel: '',
  };
  isModalOpen = false;
  isLoading = false;
  statusMessage: any;
  allContacts: IContact[] = [];
  hasMore = true;
  // __________________________________________________________
  dropdownOpen = false;
  // Nouvelles propriétés pour les templates
  showTemplates: boolean = false;
  templates: any[] = [];
  loadingTemplates: boolean = false;
  templateSearch: string = '';
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  selectedTemplate: any = null;
  contactsWithMissingChannels: Array<{ contact: IContact; missingChannels: string[] }> = [];
  // ____________________________________________________________

  constructor(
    private chatservice: ChatService,
    private contactService: ContactService,
    private zone: NgZone,
    private changeDetectorRef: ChangeDetectorRef,
    private smsService: SmsService,
    private templateService: TemplateService,
  ) {}

  ngOnInit(): void {
    this.loadChats();
    this.loadContacts();
    // Handle search with debounce
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(term => {
      this.searchTerm = term;
      this.filterGroupedChats();
    });
  }
  loadContacts(search: string = '', page: number = 0): void {
    this.isLoading = true;
    this.contactService
      .query({ search, page, size: 20 })
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .subscribe(contacts => {
        if (page === 0) {
          this.allContacts = contacts;
        } else {
          this.allContacts = [...this.allContacts, ...contacts];
        }
        this.hasMore = contacts.length === 20;
        this.isLoading = false;
      });
  }
  openDropdown(): void {
    this.dropdownOpen = true;
    if (this.allContacts.length === 0) {
      this.loadContacts();
    }
  }

  onSearchChange(term: string): void {
    this.page = 0;
    this.allContacts = [];
    this.hasMore = true;
    this.loadContacts(term);
  }

  onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20 && this.hasMore && !this.isLoading) {
      this.page++;
      this.loadContacts(this.searchTerm, this.page);
    }
  }
  clearSelectedContact(): void {
    this.selectedContact = null;
  }

  ngAfterViewInit(): void {
    // Add window resize handler for responsive adjustments
    fromEvent(window, 'resize')
      .pipe(debounceTime(200), takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.messagesViewport) {
          this.messagesViewport.checkViewportSize();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }
  }

  /** Fonction pour grouper les chats par contact */
  private groupChatsByContact(chats: Chat[]): GroupedChats {
    return chats.reduce((grouped: GroupedChats, chat: Chat) => {
      const contactId = chat.contact.id;

      if (!grouped[contactId]) {
        grouped[contactId] = {
          contact: chat.contact,
          chats: [],
        };
      }

      grouped[contactId].chats.push(chat);

      return grouped;
    }, {});
  }

  selectContactChannel(contact: IContact, channel: 'WHATSAPP' | 'SMS'): void {
    let isWhatsapp = false;
    if (channel == 'WHATSAPP') {
      isWhatsapp = true;
    }
    this.loadTemplates(this.currentPage, this.pageSize, isWhatsapp);
    this.selectedContact = contact;
    this.selectedChannel = channel;
    this.messages = [];
    this.messagesPage = 0;
    this.noMoreMessages = false;
    this.messageDates.clear();
    this.chatservice.getChatsByContactIdGroupedByChannel(contact.id).subscribe(groupedChats => {
      groupedChats[channel].forEach(chat => {
        this.loadMessages(chat.chatId);
        this.selectedChatId = chat.chatId;
      });
    });
  }

  /** Charger tous les chats et les grouper par contact */
  loadChats(append: boolean = false): void {
    if (!append) {
      this.page = 0;
      this.chats = [];
      this.groupedChats = {};
      this.groupedChatsArray = [];
    }

    this.loadingChats = true;

    this.chatservice.getAllChats(this.page, this.itemsPerPage).subscribe({
      next: res => {
        const totalCountHeader = res.headers.get('X-Total-Count');
        this.totalItems = totalCountHeader ? +totalCountHeader : 0;
        this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);

        const newChats = res.body ?? [];
        this.chats = append ? [...this.chats, ...newChats] : newChats;

        // Grouper les chats par contact
        this.groupedChats = this.groupChatsByContact(this.chats);
        this.groupedChatsArray = Object.values(this.groupedChats);

        // Trier par dernière mise à jour (optionnel)
        this.groupedChatsArray.forEach(group => {
          group.chats.sort((a, b) => new Date(b.lastUpdated).getTime() - new Date(a.lastUpdated).getTime());
        });

        // Trier les groupes par le chat le plus récent de chaque contact
        this.groupedChatsArray.sort((a, b) => {
          const lastChatA = a.chats[0]?.lastUpdated || '';
          const lastChatB = b.chats[0]?.lastUpdated || '';
          return new Date(lastChatB).getTime() - new Date(lastChatA).getTime();
        });

        this.totalChats = this.totalItems;
        this.filterGroupedChats();
      },
      error: err => {
        this.loadingChats = false;
        console.error('Erreur lors du chargement des chats :', err);
      },
      complete: () => {
        this.loadingChats = false;
      },
    });
  }

  /** Recherche dans les chats groupés */
  searchChats(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchSubject.next(input.value);
  }

  /** Filtrer les chats groupés par nom, prénom et téléphone */
  private filterGroupedChats(): void {
    if (!this.searchTerm) {
      this.filteredGroupedChats = this.groupedChatsArray;
    } else {
      const term = this.searchTerm.toLowerCase();

      this.filteredGroupedChats = this.groupedChatsArray.filter(group => {
        const contact = group.contact;

        // Recherche dans le nom du contact
        const nom = (contact.connom || '').toLowerCase();
        const nomMatches = nom.includes(term);

        // Recherche dans le prénom du contact (ajustez le nom de la propriété selon votre modèle)
        const prenom = (contact.conprenom || '').toLowerCase(); // Remplacez 'conprenom' par le nom réel de votre propriété
        const prenomMatches = prenom.includes(term);

        // Recherche dans le nom complet (nom + prénom)
        const nomComplet = `${nom} ${prenom}`.trim();
        const nomCompletMatches = nomComplet.includes(term);

        // Recherche dans le téléphone du contact
        let phoneStr = '';
        if (typeof contact.contelephone === 'string') {
          phoneStr = contact.contelephone;
        } else if (contact.contelephone && typeof contact.contelephone === 'object') {
          phoneStr = contact.contelephone.e164Number || contact.contelephone.nationalNumber || '';
        }
        const phoneMatches = phoneStr.toLowerCase().includes(term);

        // Recherche dans les numéros de téléphone formatés (sans espaces, tirets)
        const cleanPhone = phoneStr.replace(/[\s\-\(\)\.]/g, '');
        const cleanPhoneMatches = cleanPhone.includes(term.replace(/[\s\-\(\)\.]/g, ''));

        // Recherche dans les channels des chats
        const channelMatches = group.chats.some(chat => chat.channel?.toLowerCase().includes(term));

        return nomMatches || prenomMatches || nomCompletMatches || phoneMatches || cleanPhoneMatches || channelMatches;
      });
    }
  }

  /** Pagination pour les chats */
  onChatsScrollIndexChange(index: number): void {
    if (index > this.groupedChatsArray.length - 3 && !this.loadingChats && this.page + 1 < this.totalPages) {
      this.page++;
      this.loadChats(true);
    }
  }

  /** Sélectionner un contact */
  selectContact(contact: IContact): void {
    this.selectedContact = contact;
    this.messages = [];
    this.messagesPage = 0;
    this.noMoreMessages = false;
    this.messageDates.clear();
    this.onContactChange();
  }

  /** Sélectionner un chat spécifique */
  selectChat(chat: Chat): void {
    this.selectedContact = chat.contact;
    this.messages = [];
    this.messagesPage = 0;
    this.noMoreMessages = false;
    this.messageDates.clear();
    this.loadMessages(chat.id);
  }

  /** Obtenir les chats d'un contact */
  getChatsForContact(contactId: number): Chat[] {
    return this.groupedChats[contactId]?.chats || [];
  }

  /** Vérifier si un contact a des chats WhatsApp */
  hasWhatsAppChat(contact: IContact): boolean {
    // this.loadTemplates(this.currentPage,this.pageSize,true);
    if (!contact.id) return false;
    const chats = this.getChatsForContact(contact.id);
    return chats.some(chat => chat.channel === 'WHATSAPP');
  }

  /** Vérifier si un contact a des chats SMS */
  hasSMSChat(contact: IContact): boolean {
    // this.loadTemplates(this.currentPage,this.pageSize,false);
    if (!contact.id) return false;
    const chats = this.getChatsForContact(contact.id);
    return chats.some(chat => chat.channel === 'SMS');
  }

  /** Obtenir le dernier chat d'un contact */
  getLastChatForContact(contact: IContact): Chat | undefined {
    if (!contact.id) return undefined;
    const chats = this.getChatsForContact(contact.id);
    return chats.length > 0 ? chats[0] : undefined;
  }

  /** Charger les messages d'un chat */
  loadMessages(chatId: number, page = 0, size = 20): void {
    this.messages = [];
    this.loadingMessages = true;

    this.chatservice.getMessagesByChatId(chatId).subscribe({
      next: (res: HttpResponse<Sms[]>) => {
        this.messages = res.body ?? [];
        this.totalMessages = Number(res.headers.get('X-Total-Count'));
        console.log('Messages reçus :', this.messages);

        setTimeout(() => this.scrollToBottom(), 100);
        console.log('messages ', this.messages);
      },

      error: err => {
        console.error('Erreur lors du chargement des messages :', err);
      },
      complete: () => {
        this.loadingMessages = false;
      },
    });
  }

  /** Track function for ngFor optimization */
  trackByContactId(index: number, group: { contact: IContact; chats: Chat[] }): number {
    return group.contact.id || index;
  }

  trackByChatId(index: number, chat: Chat): number {
    return chat.id || index;
  }

  trackByMessageId(index: number, message: Sms): number {
    return message.id || index;
  }

  /** Check if we should show date separator */
  showDateSeparator(message: Sms, index: number): boolean {
    if (!message.sendDate) return false;
    if (index === 0) return true;

    const currentDate = new Date(message.sendDate);
    const previousMessage = this.messages[index - 1];

    if (!previousMessage?.sendDate) return true;

    const previousDate = new Date(previousMessage.sendDate);

    return currentDate.toDateString() !== previousDate.toDateString();
  }

  /** Get message date as Date object */
  getMessageDate(message: Sms): Date {
    if (message.id && this.messageDates.has(message.id)) {
      return this.messageDates.get(message.id)!;
    }
    return new Date(message.sendDate || Date.now());
  }

  onMessageScroll(): void {
    if (!this.messagesViewport) return;

    const scrollOffset = this.messagesViewport.measureScrollOffset('top');
    const maxScroll = this.messagesViewport.measureScrollOffset('bottom');
    this.showScrollButton = maxScroll > 20;

    this.isScrolling = true;
    if (this.scrollTimeout) {
      clearTimeout(this.scrollTimeout);
    }

    this.scrollTimeout = setTimeout(() => {
      this.isScrolling = false;
      this.changeDetectorRef.detectChanges();
    }, 200);
  }

  /** Scroll to bottom of messages */
  scrollToBottom(): void {
    if (this.messagesViewport) {
      this.zone.runOutsideAngular(() => {
        setTimeout(() => {
          this.messagesViewport.scrollTo({ bottom: 0, behavior: 'smooth' });
          this.showScrollButton = false;
        }, 0);
      });
    }
  }

  onContactChange(): void {
    this.formData.channel = '';
    if (this.selectedContact?.id) {
      const selectedContact = this.contactsWithMissingChannels.find(e => e.contact.id?.toString() === this.selectedContact?.id?.toString());
      this.availableChannels = selectedContact?.missingChannels || [];
    } else {
      this.availableChannels = [];
    }
  }

  openModal(): void {
    this.isModalOpen = true;

    const allChannels = ['WHATSAPP', 'SMS'];
    const groupedContactMap = new Map<number, Chat[]>(); // Map contact.id -> chats

    // Construire une map des chats par contact à partir de groupedChatsArray
    this.groupedChatsArray.forEach(group => {
      groupedContactMap.set(group.contact.id, group.chats);
    });

    this.allContacts.forEach(contact => {
      const chats = groupedContactMap.get(contact.id) || []; // s’il n’y en a pas, c’est []
      const channels = chats.map(chat => chat.channel);

      // Trouver les channels manquants
      const missingChannels = allChannels.filter(channel => !channels.includes(channel));

      if (missingChannels.length > 0) {
        this.contactsWithMissingChannels.push({ contact, missingChannels });
      }
    });

    this.formData = {
      contactId: '',
      channel: '',
    };
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.contactsWithMissingChannels = [];
  }

  onSubmit(form: NgForm) {
    if (form.valid) {
      this.isLoading = true;

      const contactId = this.selectedContact?.id;
      const channel = this.formData.channel as 'SMS' | 'WHATSAPP';

      this.chatservice.createChat(contactId, channel).subscribe({
        next: response => {
          this.isLoading = false;
          this.isModalOpen = false;
          this.statusMessage = {
            type: 'success',
            message: 'Chat créé avec succès!',
          };

          // Recharger la liste des chats
          this.loadChats();

          // Fermer le modal après 2 secondes
          setTimeout(() => this.closeModal(), 2000);
        },
        error: error => {
          this.isLoading = false;
          this.statusMessage = {
            type: 'error',
            message: 'Erreur lors de la création du chat: ' + (error.error?.message || error.message),
          };
          console.error('Erreur création chat:', error);
        },
      });
    }
  }

  send(): void {
    if (!this.selectedContact || !this.newMessage.trim() || !this.selectedChannel) return;

    this.sending = true;

    const tempMessage: Sms = {
      id: Date.now(),
      msgdata: this.newMessage,
      template_id: this.selectedTemplate.id,
      receiver: this.selectedContact.contelephone?.toString() || '',
      sendDate: new Date().toISOString(),
      status: 'PENDING',
      type: this.selectedChannel as MessageType,
      direction: 'OUTBOUND',
      chat: { id: this.selectedChatId },
    };

    this.messages = [...this.messages, tempMessage];
    this.newMessage = '';
    this.scrollToBottom();

    // Envoi réel
    this.smsService.createSms(tempMessage).subscribe({
      next: createdSms => {
        this.messages = this.messages.map(msg => (msg.id === tempMessage.id ? createdSms : msg));
        this.sending = false;
      },
      error: error => {
        this.messages = this.messages.map(msg => (msg.id === tempMessage.id ? { ...msg, status: 'FAILED' } : msg));
        this.sending = false;
        console.error("Erreur lors de l'envoi:", error);
      },
    });
  }

  toggleTemplates() {
    this.showTemplates = !this.showTemplates;
  }

  loadTemplates(page: number, size: number, iswhatsapp: boolean) {
    this.templateService.getAllTemplates(page, size, undefined, iswhatsapp).subscribe({
      next: response => {
        this.templates = response.content || [];
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || 0;
        this.loadingTemplates = false;
      },
      error: error => {
        console.error('Erreur lors du chargement des templates:', error);
        this.loadingTemplates = false;
        // Vous pouvez ajouter une notification d'erreur ici
      },
    });
  }

  trackByTemplate(index: number, template: any): any {
    return template.id || index;
  }

  selectTemplate(template: any) {
    this.newMessage = template.name;
    this.selectedTemplate = template;
    this.showTemplates = false;

    setTimeout(() => {
      const inputElement = document.querySelector('input[name="newMessage"]') as HTMLInputElement;
      if (inputElement) {
        inputElement.focus();
      }
    }, 100);
  }
}
