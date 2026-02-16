import { ChannelType } from './channel.types';

export interface ChannelConfigurationDTO {
  id?: number;
  channelType: ChannelType;
  verified?: Boolean;
  username: String;
  host: String;
  port: number;
  extraInfo?: String;
}
